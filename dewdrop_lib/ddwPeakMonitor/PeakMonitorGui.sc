
PeakMonitorGui : ObjectGui {
	var	leftFlow, rightFlow;
	var	multiSl, clipButtons, maxRecentView;
	var	maxRecent, recentSize, maxClip;
	
	guiBody { arg lay;
		lay.startRow;
		leftFlow = FlowView(lay, Rect(0, 0, model.numChannels * 13 + 10, 210));
		multiSl = GUI.multiSliderView.new(leftFlow, Rect(0, 0, model.numChannels * 13 + 2, 200))
			.size_(model.numChannels);
		
		rightFlow = FlowView(lay, Rect(0, 0,
				// ax + b(x-1) = ax + bx - b = (a+b)x - b
			max((lay.decorator.gap.x + 50) * model.numChannels - lay.decorator.gap.x + 10, 210),
			210));	// height

		clipButtons = Array.fill(model.numChannels, { arg i;
			GUI.button.new(rightFlow, Rect(0, 0, 50, 20))
				.states_([
					["ok", Color.black, Color.grey],
					["CLIP", Color.white, Color.red]
				])
				.action_({ // arg b;
					clipButtons.do({ |b, j|
						Post << "Channel " << j << " maximum clip: " << maxClip[j] << "\n";
						maxClip[j] = 0;
						b.value_(0)
					});
				});
		});
		rightFlow.startRow;
		maxRecentView = GUI.textView.new(rightFlow, Rect(0, 0, 200, 100));
		
		maxClip = 0 ! model.numChannels;
		recentSize = model.freq*2;
		maxRecent = Array.new(recentSize);
	}
	
	update {
		var newpeaks, str;
		multiSl.notClosed.if({	// if you close the window at the wrong time, this prevents a choke
			newpeaks = model.peaks.collect({ arg p, i; 
				(p > maxClip[i]).if({ maxClip[i] = p });
				((p = p.abs) > 1).if({
					{ clipButtons.at(i).value_(1) }.defer;
				});
				p.clip(0,1)
			});
				// update "recent peak" display
			(maxRecent.size >= recentSize).if({ maxRecent.removeAt(0) });
			maxRecent.add(model.peaks);
			str = "Channel peaks:\n\n";
			maxRecent.flop.do({ |chan, i|
				str = str ++ format("%: % dB\n", i+1, chan.maxItem.ampdb.round(0.01));
			});
			{	multiSl.value_(newpeaks.sqrt);
				maxRecentView.setString(str, 0, maxRecentView.string.size);
			}.defer;
		});
	}
	
	remove {
		model.free;
	}
	
}
