
SynthVoicerNode {
		// one voice of a Voicer
		// this one handles standard synthdefs
		// for other types, subclass SynthVoicerNode and override methods
		// H. James Harkins, jamshark70@dewdrop-world.net

	var	>isPlaying = false,
		<>isReleasing = false,
		<>frequency,				// so voicer can identify notes to release
		<>lastTrigger = 0,			// ditto -- time of last trigger
		<>target, <>addAction,		// for allocating nodes
		<>bus,				// output bus to pass to things
		<synth, 		// the node
		<initArgs,	// send to each node on initiation
		<defname,
//		<argDict,		// maps standard names \freq, \gate, etc to the actual arg names
		voicer,		// to help with globally mapped controls; what if voicer is nil?
		myLastLatency,	// because latency is now variable at the voicer level
						// important because you may have 2 processes with different latencies
						// using the same Voicer
		<>steal = true;		// by default, if another note needs this object, its synth node can be killed
						// false means let the node die on its own (you lose synth activity control)
	
	*new { arg thing, args, bus, target, addAction = \addToTail, voicer, defname;
		target = target.asTarget;		// if nil, gives default server
		^super.new.init(thing, args, bus, target, addAction, voicer, defname)
	}
	
	init { arg th, ar, b, targ, addAct, par;
		synth.notNil.if({ this.free });		// if re-initing, drop synth node
		target = targ;		// save arguments
		addAction = addAct;
		
			// remove arg pairs from ar whose key is \freq, \gate or \outbus
		initArgs = this.makeInitArgs(ar);
		
		voicer = par;
		defname = th;

			// use given bus or hardware output
		bus = b ? Bus.new(\audio, 0, 1, target.server);
//		argDict = IdentityDictionary.new;
		^this
	}
	
	dtor {
		this.free
	}

	makeInitArgs { arg ar;
		var out;
		out = Array.new;
		ar.pairsDo({ |name, value|
			(#[\freq, \freqlag, \gate, \t_gate, \out, \outbus].includes(name).not and:
					{ this.testArgClass(value) })
				.if({
				out = out ++ [name, value];
			});
		});
		^out
	}
	
		// this test is split out of the above in case future subclasses need a different test
	testArgClass { |argValue| ^argValue.isNumber }

	triggerMsg { arg freq, gate = 1, args;
		var bundle, args2;
			// create osc message
		bundle = List.new;
			// assemble arguments
		args2 = initArgs ++ [\gate, gate, \t_gate, gate];
		args = (args ? []).flat;
		(args.at(0).notNil).if({ args2 = args2 ++ args });
		freq.notNil.if({ args2 = args2 ++ [\freq, freq] });
			// make synth object
		synth = Synth.basicNew(defname, target.server);
		bundle.add(synth.newMsg(target, args2 ++ this.mapArgs
			++ [\out, bus.index, \outbus, bus.index], addAction));
//bundle.asCompileString.postln;
		^bundle
	}
	
	triggerCallBack { ^nil }	// this is what OSCSchedule uses for its clientsidefunc
							// InstrVoicerNode uses this

	trigger { arg freq, gate = 1, args, latency;
		var bundle;
//		this.shouldSteal.if({
////			"Stealing node.".postln;
//			bundle = this.releaseMsg(-1);	// release the node immediately
//		}, {
//			bundle = Array.new;
//		});
		this.shouldSteal.if({
//			"Stealing node.".postln;
			this.stealNode(synth, latency);
		});
		bundle = this.triggerMsg(freq, gate, args);
//bundle.asCompileString.postln;
		target.server.listSendBundle(myLastLatency = latency, bundle/*.debug("% trigger".format(thisThread.clock.beats))*/);
		NodeWatcher.register(synth);
			// when the synth node dies, I need to set my flags
		Updater(synth, { |syn, msg|
			(msg == \n_end).if({
					// synth may have changed
				(syn == synth).if({
					isPlaying = isReleasing = false;
				});
				syn.releaseDependants;	// remove node and Updater from dependants dictionary
			});
		});
		frequency = freq;	// save frequency for Voicer.release
		lastTrigger = Main.elapsedTime;	// save time
		isPlaying = true;
		isReleasing = false;
	}
	
	shouldSteal {
		^steal and: { isPlaying or: { synth.notNil and: { synth.isPlaying } }
		or: { Main.elapsedTime - lastTrigger < (myLastLatency ? 0) } }
	}
	
		// must pass in node because, when a node is stolen, my synth variable has changed
		// to the new node, not the old one that should go away
	stealNode { |node, latency|
		node.server.sendBundle(latency, #[error, 0], node.setMsg(\gate, -1), #[error, 1]);
	}
	
	releaseMsg { arg gate = 0;
		^[#[error, 0], [15, synth.nodeID, \gate, gate]/*, #[error, 1]*/]
	}
	
	releaseCallBack {
		^nil
	}
	
		// release using Env's releaseNode
		// freq argument is because scheduled releases may be talking to a node that's been stolen.
		// In that case, the frequency will be different and the release should not happen.
		// if left nil, the release will go ahead.
	release { arg gate = 0, latency, freq;
		this.shouldRelease(freq).if({ 
			synth.server.listSendBundle(latency, this.releaseMsg(gate)/*.debug("% release".format(thisThread.clock.beats))*/);
			isPlaying = false;
			isReleasing = true;
		});
	}
	
	isPlaying { ^isPlaying or: { (synth.notNil and: { synth.isPlaying }) } }
	
	shouldRelease { arg freq;
		^(this.isPlaying and: { freq.isNil or: { freq == frequency } })
	}
	
	releaseNow { arg sec = 0;	// release immediately using linear decay
		this.release(sec.abs.neg - 1);	// -1 = instant decay, -0.5-1 = -1.5 = .5 sec decay
	}
	
	freeMsg {
		^[[11, synth.nodeID]]
	}
	
	freeCallBack { ^nil }

	free {	// remove from server; assumes envelope is already released
		(this.isPlaying).if({ synth.free; });
		isPlaying = false;
	}
	
	setMsg { arg args;
		var ar, bundle;
		this.isPlaying.if({
				// ignore global controls (handled by Voicer.set)
			args = (args ? []).clump(2)
				.select({ arg a; voicer.globalControls.at(a.at(0).asSymbol).isNil })
				.flatten(1);
			^[[15, synth.nodeID] ++ args]
		}, {
			^nil
		});
	}
	
	setCallBack { ^nil }

	set { arg args, latency;
		(this.isPlaying).if({
			target.server.listSendBundle(latency, this.setMsg(args)/*.debug("% set".format(thisThread.clock.beats))*/);
		});
	}
	
	setArgDefaults { |args|
		var	index;
		args.pairsDo({ |key, value|
			(index = initArgs.indexOf(key)).notNil.if({
				initArgs[index + 1] = value;
			}, {
				initArgs = initArgs.add(key).add(value);
			});
		})
	}
	
// GENERAL SUPPORT METHODS
	server { ^target.server }	// tell the outside world where I live

//	translateArgs { arg ... args;
//		var	newarg;
//			// looks up symbols in argDict and substitutes synthdef arg names
//		args = args.flat;
//			// if args is smaller than argDict, loop thru args
//		((args.size / 2) < argDict.size).if({
//			forBy(0, args.size-1, 2, {	// go thru in pairs
//				arg i;
//				newarg = argDict.at(args.at(i));
//				newarg.notNil.if({ args.put(i, newarg) });
//			});
//		}, {		// otherwise loop thru argDict
//			argDict.keysValuesDo({ arg k, v;
//				newarg = args.indexOf(k);
//				newarg.notNil.if({ args.put(newarg, v) });
//			});
//		});
//		^args
//	}
//		
	map { arg name, bus;	// do mapping for this node
		synth.notNil.if({
			synth.map(name, bus);
		});
	}
	
	mapArgsMsg { // assumes synth is loaded
		var mapMsg;
			// if nothing's in the globalControls dictionary, no need to do anything
		(voicer.globalControls.size > 0).if({
			mapMsg = [14, synth.nodeID];	// message header
			voicer.globalControls.keysValuesDo({ arg name, gc;
				mapMsg = mapMsg ++ [name, gc.bus.index];  // yep, add it to msg
			});
			^[mapMsg]
		});
		^nil		// if nothing to map
	}
	
	mapArgs { // assumes synth is loaded
		var out;
			// if nothing's in the globalControls dictionary, no need to do anything
		(voicer.globalControls.size > 0).if({
			out = Array.new(voicer.globalControls.size * 2);
			voicer.globalControls.keysValuesDo({ arg name, gc;
				out.add(name);
				out.add(("c" ++ gc.bus.index).asSymbol);
			});
			^out
		});
		^nil		// if nothing to map
	}
	
	displayName { ^defname }
}

InstrVoicerNode : SynthVoicerNode {
		// children are InstrVoicerNodes for any sub-patches
	var	<patch, <instr, <children;
	
	init { arg th, ar, b, targ, addAct, par, olddefname;
		var	def;
		patch.notNil.if({ this.free });		// if re-initing, drop synth node
		target = targ;		// save arguments
		instr = th;
		voicer = par;
		addAction = \addToTail;		// must always be so for compound patches
			// use given bus or hardware output
		bus = b ? Bus.new(\audio, 0, 1, target.server);
		olddefname.isNil.if({
			target.server.waitForBoot({	// don't make the patch until server is ready
				patch = this.makePatch(instr, ar);
					// cracky workaround for cxx's synthdef naming problem
					// this breaks nested patches but I never use that anyway
				def = patch.asSynthDef;
				def.name = (defname = def.name ++ UniqueID.next);
				def.send(target.server);
//				def.memStore;		// this might not work with wrapped Instr's
//				patch.prepareForPlay(target, false, bus);
//				defname = patch.defName;
//				patch = nil;		// garbage collect patch
			});
		}, {
				// olddefname was not nil: the patch was made earlier by another node
				// this one will use the same synthdef
			defname = olddefname;
			initArgs = this.makeInitArgs(ar);
		});
	}
	
	dtor {
		super.dtor;	// release synth nodes
		patch.free;	// garbage collect patch
		patch = nil;
	}
	
	makePatch { |instr, args|
		^PatchNoDep.new(instr, this.makePatchArgs(instr, args))
	}
	
		// does trigger et al. need to hit the children?
	trigger { arg freq, gate = 1, args, latency;
		var bundle;
//["InstrVoicerNode-trigger", synth.tryPerform(\nodeID), this.shouldSteal].postln;
//		this.shouldSteal.if({
////			"Stealing node.".postln;
//			bundle = this.releaseMsg(-1);
////bundle.asCompileString.postln;
//		}, {
//			bundle = Array.new
//		});
//["InstrVoicerNode-trigger - bundle before triggering:", bundle].postln;
		this.shouldSteal.if({
//			"Stealing node.".postln;
			this.stealNode(synth, latency);
		});
		bundle = bundle ++ this.triggerMsg(freq, gate, args);
//bundle.asCompileString.postln;
		target.server.listSendBundle(myLastLatency = latency, bundle/*.debug("% trigger".format(thisThread.clock.beats))*/);
		
		frequency = freq;
		lastTrigger = Main.elapsedTime;
	}
	
	triggerMsg { arg freq, gate = 1, args;
		var bundle;
		bundle = Array.new;
			// make messages for children
		children.do({ arg child; bundle = bundle ++ child.triggerMsg(freq, gate, args); });
		
		bundle = bundle ++ super.triggerMsg(freq, gate, args);
			// super.triggerMsg also handles global mapping
		NodeWatcher.register(synth);  // we now have a synth object too
			// when the synth node dies, I need to set my flags
		Updater(synth, { |syn, msg|
			(msg == \n_end).if({
				(syn == synth).if({
					isPlaying = isReleasing = false;
				});
				syn.releaseDependants;	// remove node and Updater from dependants dictionary
			});
		});

		isPlaying = true;
		isReleasing = false;
		^bundle
	}
	
	triggerCallBack { ^nil }	// patch updating can be ignored

	release { arg gate = 0, latency, freq;
		this.shouldRelease(freq).if({
			this.target.server.listSendBundle(latency, this.releaseMsg(gate));
//			this.set([\gate, gate], latency);
			isPlaying = false;
			isReleasing = true;
		});
	}

		// superclass takes care of this
//	releaseNow { arg sec = 0;
//		this.release(sec.abs.neg - 1, sec);
//	}
	
		// releaseMsg assumes that the caller has done all the safety checks
	releaseMsg { arg gate = 0, wrap = true;	// wrap with \error msg?
		var bundle;
//["InstrVoicerNode-releaseMsg", synth.tryPerform(\nodeID), gate].postln;
		wrap.if({
			bundle = [#[\error, 0]];
		}, {
			bundle = Array.new;
		});
		children.do({ |child| bundle = bundle ++ child.releaseMsg(gate, false); });
		bundle = bundle ++ [[15, synth.nodeID, \gate, gate]];
			// not needed because ['/error', 0] is now local to the bundle
//		wrap.if({ bundle = bundle ++ [#[error, 1]]; });
//["InstrVoicerNode-releaseMsg - message: ", bundle].postln;
		^bundle
	}
	
	releaseCallBack { arg gate;
		^nil
	}

	freeMsg {
		var bundle;
		(synth.notNil and: { synth.isPlaying }).if({
			isPlaying = false;
			bundle = List.new;
				// collect free messages for children
			children.do({ arg child; bundle = bundle ++ child.freeMsg; });
			^bundle ++ [[11, synth.nodeID]];
		}, {
			^[]	// if synth isn't playing, freeMsg is meaningless
		});
	}
	
	freeCallBack {
		^nil
	}

	set { arg args, latency;
		synth.notNil.if({
			target.server.listSendBundle(latency, this.setMsg(args)/*.debug("% set".format(thisThread.clock.beats))*/);
		});
		this.setCallBack.value(args);
	}

	setMsg { arg args;
		var bundle, ar, argColl;
		synth.notNil.if({
			bundle = Array.new;
				// collect set messages for children
			children.do({ arg child; 
				bundle = bundle ++ child.setMsg(args) 
			});

//			ar = Array.new;
//				// select only non-global controls
//			forBy(0, args.size-1, 2, { arg i;
//				voicer.globalControls.at(args.at(i).asSymbol).isNil.if({
//					ar = ar ++ [args.at(i), args.at(i+1)];
//				});
//			});
//			ar = this.translateArgs(ar);
////			argColl = Array.new;
////			ar.clump(2).do({ arg a;
////				a.at(0).isInteger.if({
////					argColl = argColl ++ a;
////				});
////			});
			^bundle ++ /* (argColl.size > 0).if({ */
				super.setMsg(args)
//			})
		}, {
			^[]	// if synth isn't playing, setMsg is meaningless
		});
	}
	
	setCallBack {
		^nil
	}			

	free {
		var bundle;
//		bundle = List.new;
//			// superclass does freeMsg
//		children.do({ arg child; bundle.add(child.freeMsg) });
//		bundle.add(this.freeMsg);
		target.server.listSendBundle(nil, this.freeMsg/*.debug("% free".format(thisThread.clock.beats))*/);
		isPlaying = false;
	}
	
	displayName { ^instr.name.asString }

// PRIVATE
	
	mapArgsMsg { 		// collects mapArgsMsgs for this and children
		var bundle;
		bundle = Array.new;
		children.do({ arg child; bundle = bundle ++ child.mapArgsMsg });
		bundle = bundle ++ super.mapArgsMsg;	// use SynthVoicerNode.mapArgsMsg for the meat
		^bundle
	}
	
	map { arg name, bus;
		children.do({ arg child; child.map(name, bus) });
		synth.notNil.if({
			synth.map(name, bus);
		});
	}

		// create dictionary for turning arg names into indices
//	makeArgDict {
//		var j;
//		argDict = IdentityDictionary.new;
//		
//		instr.func.def.argNames.do({ arg name, i;
//			j = patch.synthArgsIndices.at(i);
//			j.notNil.if({
//				argDict.put(name, j);
//			});
//		});
//	}
//
	makePatchArgs { arg instr, ar;
		var	argNames, argSpecs, argArray, proto,
			argIndex, gateIndex, thisArg, basePatch, temp;
		
			// to support instr wrapping, I need to know what args are created during def building
		try {
			(basePatch = PatchNoDep(instr)).asSynthDef;  // can throw this one away
			argNames = basePatch.argNames;
			argSpecs = basePatch.argSpecs;
		} {
			argNames = instr.func.def.argNames;
			argSpecs = instr.specs;
		};
		basePatch.free;	// remove dependents
		initArgs = Array.new;

			// if no args, make empty array
		ar = ar ? [];
		argArray = argNames.collect({ arg name, i;
			argIndex = ar.indexOf(name);	// find specified value, if any
			thisArg = argIndex.isNil.if({ nil }, { ar.at(argIndex+1) });
//[name, argIndex, thisArg].debug("arg");

			switch(name)
				{ \gate } { KrNumberEditor(thisArg ? 0, argSpecs[i]).lag_(nil) }
				{ \t_gate } { SimpleTrigger(argSpecs[i]) }
			
			{		// if you're nesting a patch, and the inner patch has a gate arg,
					// and the outer one does not, use inner patch as triggerable
// I may remove this support because it never worked well
				(thisArg.isKindOf(Instr)).if({
						// make inner patch
					proto = InstrVoicerNode(thisArg, ar.at(argIndex+2),
						bus, target, addAction, voicer);
					children = children.add(proto);
					proto.patch	// output new Patch as arg
				}, {
					thisArg.isNumber.not.if({
						thisArg.dereference	// Refs of SimpleNumbers for fixed args
					}, {		// otherwise make a default control (see Patch-createArgs)
						proto = argSpecs.at(i).defaultControl;
						proto.tryPerform('spec_',argSpecs.at(i)); // make sure it does the spec
						argIndex.notNil.if({
							proto.tryPerform('value_', thisArg);// set its value
						});
							// so all nodes are properly initialized at play time
							// only SimpleNumber args need to be added here; others
							// will be fixed args that we can't talk to
						(argIndex.notNil 
							and: { #[\freq, \freqlag, \gate, \t_gate, \out]
								.includes(name).not })
						.if({
							initArgs = initArgs ++ [name, thisArg];
						});
						proto
					});
				});
			};
		});

		^argArray
	}

}
