
+ Patch {
	playToMixer { arg m, atTime = nil, callback;
		var	bundle = MixedBundle.new,
			timeOfRequest = Main.elapsedTime;
		this.group = m.synthgroup;
		this.loadDefFileToBundle(bundle, m.server);
		this.prPlayToBundle(atTime,
			SharedBus(this.rate, m.inbus.index, synthDef.numChannels, m.server),
			timeOfRequest, bundle);
		callback !? { bundle.addFunction(callback) };
		bundle.sendAtTime(this.server, atTime, timeOfRequest);
	}
}

+ AbstractPlayer {
	prPlayToBundle { arg atTime, bus, timeOfRequest, bundle;
		if(status !== \readyForPlay,{ this.prepareToBundle(group, bundle, false, bus) });
		this.makePatchOut(group,false,bus,bundle);
		this.spawnToBundle(bundle);
	}
}

+ Nil {
	draggedIntoMixerGUI { |gui|
		gui.mixer = nil;
	}
	
	asMixerChannelGUI { |board|
		^MixerChannelGUI(nil, board)
	}
}

// needed to allow collections as mixer arguments in MixingBoard-new
+ Collection {
	asMixerChannelGUI { |board|
		^this.collect({ |mixer| MixerChannelGUI(mixer, board) })
	}
}

+ MixerChannel {
	draggedIntoMixerGUI { |gui|
		gui.mixer = this;
	}
}

// I have to override this core method. Sorry.
// If I don't, MixerChannelReconstructor doesn't work.
+ Server {
	initTree {
			// MixerChannelReconstructor requires NOT resetting NodeIDAllocator
//		nodeAllocator = NodeIDAllocator(clientID);	
		this.sendMsg("/g_new", 1);
		tree.value(this);
	}
}
