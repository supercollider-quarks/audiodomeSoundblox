
+ Function {
	record {arg time, bus, channels=1;
		var foundWidget = false;
		
		XQ.globalWidgetList.do({|widget| 
			if(widget.isKindOf(XiiRecorder), {
				"i've got a widget".postln;
				foundWidget = true;
				widget.record(time, bus);
			})
		});
		if(foundWidget == false, {
			XQ.globalWidgetList.add(XiiRecorder.new(Server.default, channels).record(time, bus));
		});
		this.play;
	}
}

+ SimpleNumber {
	
	// checking if a MIDI note is microtone
	midiIsMicroTone { arg tolerance = 0.01;
		if(this.frac < tolerance, {^false}, {^true});
	}
	// checking if a frequency is microtone
	freqIsMicroTone { arg tolerance = 0.01;
		if(this.cpsmidi.frac < tolerance, {^false}, {^true});
	}

	midinotename { arg sign;
		// appropriated from wouter's method, since it's not a quark...
		var out;
		if(sign.isNil) {sign = $n};
		if(sign.class == Symbol) {sign = sign.asString};
		if(sign.class == String) {sign = sign[0]};
		out = IdentityDictionary[
		$# -> ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"],
		$b -> ["C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"],
		$n -> ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"]
		].at(sign)[this.round(1.0) % 12] ++ ((this.round(1.0) / 12).floor - 2).asInt;
		^out;
	}

}

+ Array {
	midinotename { arg sign;     
		^this.collect(_.midinotename(sign));
	}
}

+ Point {
	distanceFrom { |other|
		^sqrt(([this.x, this.y] - [other.x, other.y]).squared.sum);
	}
}

+ SCEnvelopeView {
	// an Env has times in sec for each point, an EnvView has points (x,y) in the view (0 to 1)
	// this method formats that
	
	env2viewFormat_ {arg env; // an envelope of the Env class passed in
		var times, levels, timesum, lastval; 
		times = [0.0]++env.times.normalizeSum; // add the first point (at 0)
		levels = env.levels;
		timesum = 0.0;
		lastval = 0.0;
		times = times.collect({arg item, i; lastval = item; timesum = timesum+lastval; timesum});
		[\times, times.asFloat, \levels, levels.asFloat].postln;
		this.value_([times.asFloat, levels.asFloat]);
	}
	
	view2envFormat {
		var times, levels, scale, lastval, timesum;
		times = this.value[0];
		levels = this.value[1];
		times = times.drop(1);
		timesum = 0.0;
		lastval = 0.0;
		times = times.collect({arg item, i; lastval = item; timesum = lastval-timesum; timesum});
		^[levels, times];
	}
}

+ SCSlider {
			
	incrementCtrl { ^this.valueAction = this.value + 0.001 }
	decrementCtrl { ^this.valueAction = this.value - 0.001 }
	
	defaultKeyDownAction { arg char, modifiers, unicode,keycode;
		// standard keydown
		if (char == $r, { this.valueAction = 1.0.rand; ^this });
		if (char == $n, { this.valueAction = 0.0; ^this });
		if (char == $x, { this.valueAction = 1.0; ^this });
		if (char == $c, { this.valueAction = 0.5; ^this });
		if (char == $], { this.increment; ^this });
		if (char == $[, { this.decrement; ^this });
		if(modifiers == 8651009, { // check if Ctrl is down first
			if (unicode == 16rF700, { this.incrementCtrl; ^this });
			if (unicode == 16rF703, { this.incrementCtrl; ^this });
			if (unicode == 16rF701, { this.decrementCtrl; ^this });
			if (unicode == 16rF702, { this.decrementCtrl; ^this });
		}, { // if not, then normal
			if (unicode == 16rF700, { this.increment; ^this });
			if (unicode == 16rF703, { this.increment; ^this });
			if (unicode == 16rF701, { this.decrement; ^this });
			if (unicode == 16rF702, { this.decrement; ^this });
		});
		^nil		// bubble if it's an invalid key
	}

}


+ ArrayedCollection {
	
	ixiplot { arg name, bounds, discrete=false, numChannels = 1, minval, maxval, parent, labels=true, filled=true, color=XiiColors.lightgreen, step=0.1;	
	
		var plotter, txt, chanArray, unlaced, val, window, thumbsize, zoom, width, 
			layout, write=false, msresize, gui;
			
		gui = GUI.current;
		
		
		bounds = bounds ?? { parent.notNil.if({
				if(parent.respondsTo(\view)){
					parent.view.bounds
				}{
					parent.bounds
				}
			}, {
				Rect(200 ,140, 705, 410);
 			});
 		};
			
		width = bounds.width-8;
		
		name = name ? "plot";
		
		unlaced = this.unlace(numChannels);
		minval = if(minval.isArray, {
			minval.collect({|oneminval, index| oneminval ?? { unlaced[index].minItem } })
		}, {
			{minval ?? { this.minItem }}.dup(numChannels);
		});
		maxval = if(maxval.isArray, {
			maxval.collect({|onemaxval, index| onemaxval ?? { unlaced[index].maxItem } })
		}, {
			{maxval ?? { this.maxItem }}.dup(numChannels);
		});
		
		chanArray = Array.newClear(numChannels);
		if( discrete, {
			zoom = 1;
			thumbsize = max(1.0, width / (this.size / numChannels));
			unlaced.do({ |chan, j|
				chanArray[j] = chan.linlin( minval[j], maxval[j], 0.0, 1.0 );
			});
		}, {
			zoom = (width / (this.size / numChannels));
			thumbsize = 1;
			unlaced.do({ |chan, j|
				val = Array.newClear(width);
				width.do { arg i;
					var x;
					x = chan.blendAt(i / zoom);
					val[i] = x.linlin(minval[j], maxval[j], 0.0, 1.0);
				};
				chanArray[j] = val;
			});
		});
		window = parent ?? { gui.window.new( name, bounds )};

		layout = gui.vLayoutView.new( window, parent.notNil.if({
			Rect(bounds.left+4, bounds.top+4, bounds.width-10, bounds.height-10);
		}, {
			Rect(4, 4, bounds.width - 10, bounds.height - 10); 
		})).resize_(5);
		
		if(labels){
			txt = gui.staticText.new(layout, Rect( 8, 0, width, 18))
					.string_(" values: " ++ this.asString);
		};

		numChannels.do({ |i|
			plotter = gui.multiSliderView.new(layout, Rect(0, 0, 
					layout.bounds.width, layout.bounds.height - if(labels, {26}, {0}))) // compensate for the text
				.readOnly_(false)
				.drawLines_(discrete.not)
				.drawRects_(discrete)
				.isFilled_(filled)
				.indexThumbSize_(thumbsize) 
				.valueThumbSize_(1)
				.step_(step)
				.background_(Color.white)
				.colors_(Color.black, color)
				.action_({|v| 
					var curval;
					curval = v.currentvalue.linlin(0.0, 1.0, minval[i], maxval[i]);
					
					if(labels){
						txt.string_("index: " ++ (v.index / zoom).roundUp(0.01).asString ++ 
						", values: " ++ this);
					};
					if(write) { this[(v.index / zoom).asInteger * numChannels + i ]  = curval };
				})
				.keyDownAction_({ |v, char|
					if(char === $l) { write = write.not; v.readOnly = write.not;  };
				})
				.value_(chanArray[i])
				.elasticMode_(1);
			(numChannels > 1).if({ // check if there is more then 1 channel
				plotter.resize_(5);
			});
		});
		
		^window.tryPerform(\front) ?? { window }
		
	}
}

/*
(
// e = Env.new([0, 1, 0.3, 0.8, 0], [1, 3, 1, 4],'linear').plot;
 e = Env.new([0.5, 1, 0.6, 0.6, 0], [0.1, 0.3, 0.81, 0.2],'linear').plot;

//e = Env.triangle(1, 1);
//e = Env.adsr(0.02, 0.2, 0.25, 1, 1, -4);

a = SCWindow("envelope", Rect(200 , 450, 250, 100));
a.view.decorator =  FlowLayout(a.view.bounds);

b = SCEnvelopeView(a, Rect(0, 0, 230, 80))
	.drawLines_(true)
	.selectionColor_(Color.red)
	.drawRects_(true)
	.resize_(5)
	.action_({arg b; [b.index,b.value].postln})
	.thumbSize_(5)
	.env2viewFormat_(e);
a.front;


)
b.view2envFormat

*/