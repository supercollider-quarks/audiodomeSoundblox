// Schrittwechsel, a performative installation for Audiodome Soundblox 
// These are the needed classfiles.
// Please see accompanying Schrittwechsel.html for the controller/main patch.
// Implemented in 2010 by Till Bovermann
// See http://tangibleauditoryinterfaces.de/index.php/tai-applications/audiodome-soundblox/ for further details


// everything "postln" is debug output, remove if neccesary
// everything "info" is regular output

BlockPerson {
	classvar <all, <idCounter;
	
	var <>server;	
	var <synth, <>synthName, <synthParams, <stepResponder;
	var <stepBuffers;
	var <onsetBuffers;
	var <>homeBlock, <>currentBlock, <lastBlock;
	var <>visualsAddr;
	var <id;

	*new{|server, stepBuffers, homeBlock, currentBlock, visualsAddr|
		^super.new.initHome(server, stepBuffers, homeBlock, currentBlock, visualsAddr)
	}
	
	*initClass {
		all = IdentitySet[];
		idCounter = 0;
	}
	
	initHome {|aServer, aStepBuffers, aHomeBlock, aCurrentBlock, aVisualAddr|
		server = aServer;

		stepBuffers  =  aStepBuffers;
		onsetBuffers = 2.collect{
			Buffer.alloc(server, 512);
		};
		homeBlock    =    aHomeBlock;
		currentBlock = aCurrentBlock;
		lastBlock    =     homeBlock;
		visualsAddr  =   aVisualAddr;

		synthName = \BlockPerson;
		synthParams = (
			masterAmp: [0.1],
			amp: [0.1],
			interpolation: [2],
			masterMute: [0],
			mute: [0],
			onsetBufnum1: [onsetBuffers[0].bufnum],
			onsetBufnum2: [onsetBuffers[1].bufnum]
		);

		// add person to current block 
		this.currentBlock.addPerson(this);

		id = idCounter;
		idCounter = idCounter +1;
		all.add(this);
	}

	remove{
		onsetBuffers.do(_.free);
		all.remove(this);	
	}
	
	inTransit {
		^(synth.notNil.if({
			synth.isPlaying;	
		}, {
			false		
		}));
	}
	setParam {|which, val|
		synthParams[which] = val.asArray;	
		synth.notNil.if{
			synth.setn(which, val.asArray);	
		};
	}
	
	// one shot synth
	// FIXME: add different door bufnums according to current side
	transite {|to, dur = 5, dt = 1| // a Block
		// one cannot go to where he already is
		(to == this.currentBlock).if{"BlockPerson:transite :illegal move".warn; ^this};
		
		
		this.inTransit.not.if({

			// remove person from current block, and 
			currentBlock.removePerson(this, dt);
			lastBlock = currentBlock;
			
			// wait for dt seconds (and take care of server's latency)
			server.makeBundle((server.latency ? 0) + dt, {
				synth = Synth(synthName, target: server)
					.set(
						\openBufnum, lastBlock.doorOpenBufnum,
						\closeBufnum, to.doorCloseBufnum,
						\stepBufnum, stepBuffers.first.bufnum,
						\dur, dur,
						\startChan, lastBlock.out,
						\finishChan, to.out,
						\rate, 1
				);

				// Watch Synth and trigger followup actions
				synth.register(assumePlaying: true)
					.addDependant({|obj, what|
						(what == \n_end).if({
							// if synth ended, put person in block
							"add person to %\n".format(this.currentBlock).postln;
							this.currentBlock.addPerson(this);
							
							stepResponder.remove;
							
							// release dependants for garbage collection
							obj.releaseDependants;
						});	
					});
				synthParams.keysValuesDo{|key, value|
					synth.setn(key, value)
				};
				stepResponder = OSCresponderNode(nil, '/step', { |t, r, msg|
					var nodeID, pos;
					nodeID = msg[1];
					pos    = msg[3];
					
					(nodeID == synth.nodeID).if({
						"step (%, from %, to %, %)".format(id, lastBlock.id, to.id, pos).postln;
						visualsAddr.sendMsg("/footstep", id, lastBlock.id, to.id, pos)
					})
				}).add;
			});

			// set current block to the one she's heading to
			currentBlock = to;
			
		}, {
			"in transit (%)".format(this).inform;	
		})
	}

	transiteImmediate{
		synth.free;
	}
	
	goHome {|dur = 5|
		this.transite(homeBlock, dur)
	}
	
	others {
		^HomeBlock.all difference: [this];
	}
	
	*sendSynth {
		SynthDef(\BlockPerson, { 
			arg	masterAmp = 0.1, amp = 1, 
				masterMute = 1, mute = 1, 
				stepBufnum = 0, openBufnum, closeBufnum,
				startChan = 0, finishChan = 1, 
				dur = 5, rate=1, interpolation=4,
				onsetBufnum1, onsetBufnum2;

			var steps, pan, pannedSteps, open, close;
			var openLength, closeLength, minDur;
			var onsets;
			

			openLength = BufSampleRate.kr(openBufnum).reciprocal * BufFrames.ir(openBufnum);
			closeLength = BufSampleRate.kr(closeBufnum).reciprocal * BufFrames.ir(closeBufnum);
		
			// minimal operation time (open and close doors)
			minDur = openLength + closeLength + dur;
			//minDur.poll;
			open = BufRd.ar(
				1,
				openBufnum, 
				EnvGen.ar(Env([0, BufFrames.ir(openBufnum), 0], [openLength, 0]), gate: Impulse.ar(0)), 
				0, // no loop 
				interpolation
			);
			
			steps = BufRd.ar(
				1,
				stepBufnum, 
				EnvGen.ar(Env([0, 0, BufFrames.ir(stepBufnum), 0], [openLength * 0.9, BufSampleRate.kr(stepBufnum).reciprocal * BufFrames.ir(stepBufnum), 0]), gate: Impulse.ar(0)), 
				0, // no loop 
				interpolation
			) * EnvGen.ar(Env.linen(0, minDur - closeLength, 0));

			pan = EnvGen.kr(Env(
				[-1, -1, 1], 
				[openLength * 0.9, dur]
			));
			pannedSteps = Pan2.ar(steps, pan);

			close = BufRd.ar(
				1,
				closeBufnum, 
				EnvGen.ar(Env([0, 0, BufFrames.ir(closeBufnum), 0], [minDur - closeLength, closeLength, 0]), gate: Impulse.ar(0), doneAction: 2), 
				0, // no loop 
				interpolation
			);
		
			// step recognition
			// onsets = OnsetsDS.kr(steps, onsetBufnum1, onsetBufnum2, 0.5, \complex, mingap: 0.2);
			
			onsets = Onsets.kr(FFT(onsetBufnum1, steps), 0.5, \power, mingap: 20);

			// send away
			SendReply.kr(onsets, '/step', pan.linlin(-1, 1, 0.2, 0.8));
		
			Out.ar(startChan,  (open  + pannedSteps[0]) * amp * masterAmp * (1 - masterMute) * (1-mute));
			Out.ar(finishChan, (close + pannedSteps[1]) * amp * masterAmp * (1 - masterMute) * (1-mute));
		}).memStore;
	}
	
}

/////////////////////////////////////////////////

HomeBlock : SoundBlock {
	var <>server;
	var <out, <activitySynth, <>activitySynthName, <activitySynthParams;
	var <>persons, <>maxAllowedPersons = 1;
	
	var <>activityBuffers, <doorOpenBuffers, <doorCloseBuffers;
	
	// overfullAction is evaluated, if the cube reaches a state in which it contains more persons than allowed. (Only once!)
	// If the state is below, fittingAction is evaluated. 
	var <>overfullAction, <>fittingAction;
	var <isOverfull;
	
	// these are the actions that are performed on a face change.
	// invisibleAction(this), faceChangeAction(this, newFace) 
	var <>invisibleAction, <>faceChangeAction, <>cubeUpdateAction;
	
	var visualsAddr;
	
	*new{|color=\red, number=0, server, activityBuffers, doorOpenBuffers, doorCloseBuffers, outChannel = 0, visualsAddr|
		^super.new(color, number).initHome(server, activityBuffers, doorOpenBuffers, doorCloseBuffers, outChannel, visualsAddr)
	}
	
	initHome {|aServer, aBuffers, aDoorOpenBuffers, aDoorCloseBuffers, outChannel, aVisualsAddr|
		out = outChannel;

		server = aServer;

		activityBuffers = aBuffers;
		doorOpenBuffers = aDoorOpenBuffers;
		doorCloseBuffers = aDoorCloseBuffers;
		
		activitySynthName = \HomeBlock;
		activitySynthParams = (
			masterAmp: [0.1],
			rates: 1!6,
			amp: [0.1],
			interpolation: [2],
			masterMute: [0],
			mute: [0],
			dampFreq: [800]
		);
		
		visualsAddr = aVisualsAddr;
		
		persons = IdentitySet[];
	}

	setActivityParam{|which, val|
		activitySynthParams[which] = val.asArray;	
		activitySynth.notNil.if{
			activitySynth.setn(which, val.asArray);	
		};
	}

	out_{|val|
		out = val;
		activitySynth.notNil.if{
			activitySynth.set(\out, val);	
		}	
	}
	
	isActive {
		^activitySynth.notNil
	}
	
	getActive {
		this.isActive.not.if({
			server.bind{
				activitySynth = Synth(activitySynthName, [\out, out], target: server).setn(\bufnum, this.activeBufNum);
				activitySynthParams.keysValuesDo{|key, value|
					activitySynth.setn(key, value)
				}
			}
		})
	}

	transitePersonsToNext{|numPersons = 1|
		var cubes, distances;
		# cubes, distances = this.nearCubes;
		this.persons.asArray.scramble.copyRange(0, numPersons).do{|p, i|
			p.transite(cubes[i], dur: distances[i] * 8, dt: i* 1.0.rand)
		}
	}

	nearCubes{
		var others = this.others.asArray;
		var distances = others.collect{|cube| 
			((this.posX - cube.posX).squared + (this.posY - cube.posY).squared).sqrt		};
		var indices = distances.order;
		
		^[others[indices], distances[indices]]
	}


	getInactive{|dt = 1|
		activitySynth.release(dt);
		activitySynth = nil;	
	}

	// only stop in emergency, use getInactive instead.
	getInactiveImmediatly {
		activitySynth.free;
		activitySynth = nil;	
	}
	

	addPerson{|person, dt = 1|
		persons.add(person);
		this.act;
	}

	removePerson{|person, dt = 0|
		persons.remove(person);
		(persons.size <= maxAllowedPersons).if{
			isOverfull = false;
		};
		persons.isEmpty.if{ // if there's no one in the cube, it cannot be active.
			this.getInactive(dt);
		};
	}

	act{
		var personsSize = persons.size;
		// "HomeBlock:act : persons.size == %".format(persons.size).postln;
		(personsSize > maxAllowedPersons).if({
			"HomeBlock:act : more then % person in %.".format(maxAllowedPersons, this).postln;
			isOverfull.not.if{
				overfullAction.value(this, personsSize, personsSize - maxAllowedPersons);
				isOverfull = true;
			};
		}, {
			isOverfull = false;
			fittingAction.value(this, personsSize)
		});
	}

	performFaceChange {|face|
		faceChangeAction.value(this, face)
	}

	performInvisible {
		invisibleAction.value(this)
	}

	performCubeUpdate {
		//"update (% (%), x:%, y:%, r:%, vis(%)) ".format(id, upFace, posX, posY, rot, visible.binaryValue).postln;
		visualsAddr.sendMsg("/block", id, upFace, posX, posY, rot, visible.binaryValue);
		cubeUpdateAction.value(this)
	}

	others {
		^HomeBlock.all difference: [this];
	}

	pr_computeBufnum {|buffers, upface = 0| 
		^buffers[((buffers.size / 6) * upface).asInteger].bufnum;
	}
	doorOpenBufnum {
		^this.pr_computeBufnum(doorOpenBuffers,  upFace);
	}
	doorCloseBufnum {
		^this.pr_computeBufnum(doorCloseBuffers, upFace);
	}
	activeBufNum {
		^this.pr_computeBufnum(activityBuffers, upFace);
	}
	
	*sendSynth {
		SynthDef(\HomeBlock, {
			arg	out = 0, 
				amp = 0.1, mute = 0, masterAmp = 1, masterMute = 0, 
				bufnum = 0, rate = 1,//startpos = 0,
				interpolation = 2, gate= 1, dampFreq = 800;
				
			var env = EnvGen.kr(Env.asr(0.1, 1, 10), gate: gate, doneAction: 2);
			var src = BufRd.ar(
				1, 
				bufnum, 
				Phasor.ar(
					0, BufRateScale.kr(bufnum) * rate, 
					0, 
					BufFrames.kr(bufnum)
				), 
				1, 
				interpolation
			);
			src = LPF.ar(src, dampFreq);
			
			
			Out.ar(out, src * amp * masterAmp * (1 - masterMute) * (1-mute) * env);
		}).memStore;
	}
}


/////////////////////////////////////////////////////////////////////////

BlockGod {
	var <>server;
	var <>blox, <>persons, <>activityDt = 0.5;
	var activityWatcher, randomActivity;
	
	*new {|server, blox, persons|
		^super.new.initGod(server, blox, persons)
	}
	
	initGod {|aServer, aBlox, aPersons|
		
		server  =  aServer;
		blox    =    aBlox;
		persons = aPersons;
		activityWatcher = Task{loop{
			activityDt.wait;
			blox.do{|block|
				(block.persons.size > 0).if({
					block.perform([\getActive, \getInactive].wchoose([0.95, 0.05]), 1);
					activityDt.asFloat.rand.wait;
				}, {
					block.getInactive(1);
				});
			}
		}};
		randomActivity = Task{loop{
			rrand(20, 120.0).wait;
			persons.choose.currentBlock.transitePersonsToNext(1);
		}};
	}
	
	start {
		activityWatcher.play;
		randomActivity.play;
	}
	stop {
		activityWatcher.stop;
		randomActivity.stop;
	}
	muteAll {
		server.bind{
			blox.do{|block|
				block.setActivityParam(\masterMute, 1);
			};
			persons.do{|person|
				person.setParam(\masterMute, 1);
			}
		}
	}
	unmuteAll {
		server.bind{
			blox.do{|block|
				block.setActivityParam(\masterMute, 0);
			};
			persons.do{|person|
				person.setParam(\masterMute, 0);
			}
		}
	}
	ampAll {|val = 0.1|
		server.bind{
			blox.do{|block|
				block.setActivityParam(\masterAmp, val);
			};
			persons.do{|person|
				person.setParam(\masterAmp, val);
			}
		}
	}
}