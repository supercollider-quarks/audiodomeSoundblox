
      /////////////////////////////////////////////////////////
     // GLOBOL-2009 : EVERYTHING, EVERYWHERE, ALL THE TIME ///
    /////////////////////////////////////////////////////////
 
  //       ADC, JRH; Donnerstag, 1. Januar 1970 01:02:42            //
 //  NETWORKING OPENS INTERPRETER - USE 'CONNECT' AT YOUR OWN RISK //



GLOBOL {

	classvar <space, <isRunning = false, <numChannels, <>numSpeakers;
	classvar server, responder, <sender, id;
	classvar <classmethdict;
	classvar prevPreProcessor, prevNetFlag;
	
	*run { | n = 8, numOutputs = 2 |
		if(isRunning) { "GLOBOL IS RUNNING ALREADY!".warn; ^this };
		prevPreProcessor = thisProcess.interpreter.preProcessor;
		thisProcess.interpreter.preProcessor = { |string|
			if(isPermitted(this, string).not) { 
				"GLOBOL DOES NOT PERMIT THIS INPUT".warn;
			} {
				this.distribute(string); // SEND TO EVERYWHERE
				interpret(process(this, string));
			};
			"''"; // BLOCK SC REPL
		};
		numChannels = n;
		numSpeakers = numOutputs;
		Server.local.options.numAudioBusChannels = n * 128;
		space = ProxySpace.push(Server.local.reboot);
		isRunning = true;
		this.buildDict;
		// this.connect;
		id = inf.asInteger.rand;
	}
	
	*end { | time = 8 |
		thisProcess.interpreter.preProcessor = prevPreProcessor;
		space.clear(time).pop;
		isRunning = false;
		this.disconnect;
	}
	
	*buildDict {
		classmethdict = ();
		
		Class.allClasses.do { |class|
				classmethdict.put(class.name.asString
							.collect(_.toUpper).asSymbol, class.name.asString);
				class.methods.do { |method|
					classmethdict.put(
						*[method.name.asString
							.collect(_.toUpper).asSymbol, method.name.asString]
					)
				}
		};
		
	}
	
	*process { | string |
		var lines = split(string, Char.nl);
		
		string.postln; // GLOBOL REPL
		
		^join(collect(lines, { | line |
			
			var newline = String.new;
			
			line = line.replace(":?",
					".playN(%);".format((0..numSpeakers-1).wrapExtend(numChannels))
			);			
			line = line.replace(":", ".");
			
			line = " " ++ line ++ "  ";
			
			// FIND GLOBOL VARIABLES
			
			(line.size - 2).do { |index|
				var left, middle, right;
				#left, middle, right = line[index..index+2];
				
				// REPLACE UPPER WITH LOWER 
				
				if(	left.isAlpha.not 
					and: { middle.isAlpha }
					and: { middle.isUpper }
					and: { right.isAlpha.not }) 
					{
						middle = middle.toLower;
						if ( (right == $.) or: { line[index + [2, 3]].join == " =" }) {
							newline = newline ++ "~%.ar(%); ".format(middle, numChannels);
							newline = newline ++ "~" ++ middle;
							
						} {
							newline = newline ++ "~%.ar(%)".format(middle, numChannels);
						}
					} {
						newline = newline.add(middle);
					};
				};
				
				newline = replaceCaps(this, newline);
				if(shouldAddSemiColon(this, newline)) { newline = newline ++ ";" };
				
				newline
				
		}), Char.nl);
	}
	
	
	// REPLACE CAP-STRINGS WITH PROPER CLASS AND METHOD NAMES
	
	*replaceCaps { | string |
		
			var allCaps = List.new;
			var capStart, capLength = 0;
			
				string.do { |char, index|
					
					if(char.isAlphaNum and: { char.isUpper }) { 
						if (capLength == 0) { 
							capStart = index; 
						};
						capLength = capLength + 1;
					} { 
						if (capLength > 1) { 
							allCaps.add([capStart, capLength]);
						};
						capLength = 0;
					};
				}; 
				string = string.copy;
				allCaps.do { |list| 
					var start, length, bigName, smallName; 
					#start, length = list;
					
					bigName = string[start..start + length - 1].asSymbol;
					smallName = classmethdict[bigName];
					smallName !? {
						string.overWrite(smallName, start);
					};
				};
				^string
	}
	
	*shouldAddSemiColon { | string |
		string.reverseDo { |char|
			if(char.isSpace.not and: { char.isAlphaNum.not }) { ^false };
			if(char.isAlphaNum) { ^true };
		};
		^false

	}
	
	
	
	// NETWORKING //
	
	*isPermitted { | string |
		string = string.collect(_.toUpper);
		// THIS IS NOT SAFE, BUT AVERTS SOME BAD SIMPLE IDEAS. 
		^string.find("UNIXCMD").isNil
			and: { string.find("SYSTEMCMD").isNil } 
			and: { string.find("FILE").isNil } 
			and: { string.find("PIPE").isNil }
	}
	
	*distribute { | string |
		sender !? { sender.sendMsg("/GLOBOL-2009", string, id) };
	}
	
	*connect { |broadcastIP|
		("* NETWORKING OPENS INTERPRETER - USE 'CONNECT' AT YOUR OWN RISK"
		"\nUSE 'DISCONNECT' TO CLOSE INTERPRETER. *").postln;
		sender = if(broadcastIP.isNil) { 
			this.broadcast 
		} {
		 	NetAddr(broadcastIP, NetAddr.langPort)
		 };
		prevNetFlag = NetAddr.broadcastFlag;
		NetAddr.broadcastFlag = true;
		
		responder = OSCresponder(nil, "/GLOBOL-2009", { |r,t,msg|
				var code = msg[1].asString;
				var inID = msg[2];
				// GLOBOL DOES TRY NOT TO PERMIT SYSTEM CALLS
				// AVOID INFINITE NETWORK LOOP
				if(inID != id and: { isPermitted(this, code) }) 
				{
					interpret(process(this, code));
				}
		}).add;
	
	}
	
	*disconnect {
		responder.remove;
		responder = nil;
		"* INTERPRETER CLOSED. *".postln;
		sender !? { 
			sender.disconnect; 
			NetAddr.broadcastFlag = prevNetFlag;
		};
	}
	
	*broadcastIP {
		var line, pipe;
		pipe = Pipe("ifconfig | grep broadcast | awk '{print $NF}'", "r");
		{ line = pipe.getLine }.protect { pipe.close };
		^line
	}
	
	*broadcast { | port = 57120, prefix = "" |
		var hostname = this.broadcastIP(prefix);
		if(hostname.isNil) { 
			hostname = "127.0.0.1"; 
			"no network with broadcast available."
			" provisionally used loopback instead.".collect(_.toUpper).warn;
		};
		^NetAddr(hostname, port)
	}

}

