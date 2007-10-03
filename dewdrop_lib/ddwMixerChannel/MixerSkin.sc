
MixerGUIDef {
	var	<>channelSize,
		<>viewProtos,	// prototypes for mixer control guis
		<>viewBounds,	// bounds relative to channel origin
		<>numPresendsShow = 1,
		<>numPostsendsShow = 1,
		<>color1, <>color2;

	*initClass {
		Class.initClassTree(Collection);
		Class.initClassTree(MixerChannelDef);
			// generic mixer gui definition (vertical)
		MixerChannelGUI.defaultDef = MixerGUIDef(Point(50, 290),
			[MixerMuteWidget, MixerRecordWidget, MixerPresendWidget, MixerPanWidget,
				MixerLevelSlider, MixerLevelNumber, MixerPostsendWidget, MixerNameWidget,
				MixerOutbusWidget],
			[Rect(0, 0, 20, 20), Rect(30, 0, 20, 20), Rect(0, 25, 50, 30), Rect(0, 65, 50, 15),
				Rect(10, 85, 30, 100), Rect(0, 190, 50, 15), Rect(0, 210, 50, 30),
				Rect(0, 245, 50, 20), Rect(0, 270, 50, 20)]);
			// 1x1 mixer has no pan control, so I should not define a pan widget
		MixerChannelDef.at(\mix1x1).guidef = MixerGUIDef(Point(50, 290),
			[MixerMuteWidget, MixerRecordWidget, MixerPresendWidget,
				MixerLevelSlider, MixerLevelNumber, MixerPostsendWidget, MixerNameWidget,
				MixerOutbusWidget],
			[Rect(0, 0, 20, 20), Rect(30, 0, 20, 20), Rect(0, 25, 50, 30),
				Rect(10, 85, 30, 100), Rect(0, 190, 50, 15), Rect(0, 210, 50, 30),
				Rect(0, 245, 50, 20), Rect(0, 270, 50, 20)]);
	}

	*new { |channelSize, viewProtos, viewKeys, viewBounds|
		^super.newCopyArgs(channelSize, viewProtos, viewKeys, viewBounds).init
	}
	
	init {
		color1 = Color.new255(84, 31, 77);
		color2 = Color.new255(230, 109, 225);
	}
	
	makeViews { |layout, origin, mixer, mcgui|
		var	views, preSendIndex = 0, postSendIndex = 0;
		views = Array.new(viewProtos.size);
		{	viewProtos.size.do({ |i|
				views.add(viewProtos[i].new(layout, viewBounds[i].moveBy(origin.x, origin.y),
					mixer, mcgui, this, preSendIndex, postSendIndex));
			});
			switch(views.last.sendType)
				{ \pre } { preSendIndex = preSendIndex + 1 }
				{ \post } { postSendIndex = postSendIndex + 1 };
		}.defer;
		^views
	}

		// needed for changing gui defs on the fly (see MixerChannelGui-guidef_)
	== { arg that;
		if(this === that,{ ^true });
		if(this.class !== that.class,{ ^false });
		this.instVarSize.do({ arg i;
			if(this.instVarAt(i) != that.instVarAt(i),{ ^false });
		});
		^true
	}
}

MixerSkin {
	// holds display parameters for a MixingBoard
	// currently allows changing sizes of controls
	// will allow custom colors etc.
	// H. James Harkins - jamshark70@yahoo.com

	classvar <>screenHeight,	// if default height is wrong, you can change it for
			<>screenWidth;	// all skins
	
	var	<>gap,				// window margins
		<>maxSize,			// of whole window
		<>maxAcross = inf;		// maximum channels across screen

	*initClass {
		Class.initClassTree(PageLayout);
		screenHeight = PageLayout.screenHeight;	// crib height from felix!! Thanks!
		screenWidth = PageLayout.screenWidth;
	}
	
	*new {
		^super.new.init;	// design your skin using setters
	}
	
	init {
		gap = Point(4, 4);
		maxSize = Point(screenWidth, screenHeight);
	}
}


///////////////// Default mixer gui widgets /////////////////

MixerWidgetBase {
	classvar	>font;
	var	<>mixer, <>gui, <>view, <>spec;

	*initClass {
		StartUp.add({ font = GUI.font.new("Helvetica", 9) });
	}
	
		// you might have switched to a different gui scheme since initializing the class library
	*font {
		(GUI.font === font.class).if({ ^font }, {
			^(font = GUI.font.new(font.name, font.size))
		});
	}

	*new { |layout, bounds, mixer, gui|
		^super.new.init(layout, bounds, mixer, gui)
	}

	init { |layout, bounds, mix, mcgui|
		mixer = mix;
		gui = mcgui;
		this.makeView(layout, bounds);
		view.action_({ |view| this.checkDoAction(view) });
	}
		// define makeView, doAction, updateView
	checkDoAction { |view|
		mixer.notNil.if({ this.doAction(view) });
	}
	refresh { |bounds|
		view.bounds = bounds;
		mixer.notNil.if({
			this.restoreView;
			this.update;
		}, {
			view.isActive.if({ this.clearView; });
		});
	}
	update { |value|
		view.isActive.if({
			mixer.notNil.if({
				this.updateView(value);
			}, {
				view.value_(this.defaultValue);
			});
		});
	}
	free {
		view.remove;
	}
	sendType { ^nil }
	restoreView {}
	defaultValue { ^0 }
}

MixerMuteWidget : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.button.new(layout, bounds)
			.states_([["M", Color.black, Color.green], ["X", Color.black, Color.red]])
	}
	doAction { |view|
		mixer.mute(view.value > 0, false);
	}
	updateView { |value|
		view.value_(value ?? { mixer.muted.binaryValue });
	}
	clearView { 
		view.value_(0);
	}
	updateKeys { ^\mute }
}

MixerRecordWidget : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.button.new(layout, bounds)
			.states_([["o", Color.red, Color.white], ["||", Color.black, Color.red]])
	}
	doAction { |view|
		(view.value > 0).if({
			mixer.unpauseRecord;
		}, {
			mixer.pauseRecord;
		});
	}
	updateView { |value|
		view.value_(value ?? { mixer.isRecording.binaryValue });
	}
	clearView { 
		view.value_(0);
	}
	updateKeys { ^\record }
}

// not a subclass because it needs some different parameters

MixerPresendWidget {
	classvar	<>defaultSliderBounds, <>defaultMenuBounds, >font;
	var	<>mixer, <>gui, <slider, <menu, <>index, <>spec, oldValue;
	var	<sliderBounds, <menuBounds, <updateKeys;

	*initClass {
		defaultSliderBounds = Rect(0, 0, 50, 5);
		defaultMenuBounds = Rect(0, 10, 50, 20);
		StartUp.add({ font = GUI.font.new("Helvetica", 9) });
	}

	*font {
		(GUI.font === font.class).if({ ^font }, {
			^(font = GUI.font.new(font.name, font.size))
		});
	}

	sendType { ^\pre }

	*new { |layout, bounds, mixer, gui, def, sendIndex|
		^super.new.init(layout, bounds, mixer, gui, def, sendIndex)
	}

	init { |layout, bounds, mix, mcgui, def, sendIndex|
		sliderBounds = defaultSliderBounds;
		menuBounds = defaultMenuBounds;
		mixer = mix;
		gui = mcgui;
		index = sendIndex;
		spec = \amp.asSpec;
		updateKeys = [(this.sendType ++ "send" ++ index).asSymbol];
		this.makeView(layout, bounds);
	}

	makeView { |layout, bounds|
		slider = GUI.slider.new(layout, this.getSliderBounds(bounds))
			.action_({ |view| this.doSliderAction(view) });
		menu = GUI.popUpMenu.new(layout, this.getMenuBounds(bounds))
			.action_({ |view| this.doMenuAction(view) })
			.items_(gui.menuItems)
			.value_(gui.menuItems.size-1)
			.font_(this.class.font);
		oldValue = menu.value;
	}
	
	getSliderBounds { |guiBounds|
		^sliderBounds.moveBy(guiBounds.left, guiBounds.top)
	}
	getMenuBounds { |guiBounds|
		^menuBounds.moveBy(guiBounds.left, guiBounds.top)
	}

	doSliderAction { |view|
		mixer !? {
			mixer.preSends[index].tryPerform('level_', 
				spec.map(view.value), false)
		}
	}
	doMenuAction { |view|
		mixer !? {
			(menu.value != oldValue).if({
					// if old value is "none," create a presend
				(oldValue == (menu.items.size-1)).if({
						// default level is 0
					"Making presend".postln;
					MixerPreSend.new(mixer, menu.value, 0);
					slider.setProperty(\value, 0);
				}, {
						// if new value is "none," free the presend
					(menu.value == (menu.items.size-1)).if({
						("Freeing presend " ++ index).postln;
						(mixer.preSends[index]).free;
					}, {
						("Repatching send from bus " ++
							(mixer.preSends[index]).outbus.index ++ " to " ++ 
							menu.value).postln;
							// otherwise, just change the destination
						(mixer.preSends[index]).outbus = menu.value;
					});
				});
			});
				// save for next call
			oldValue = menu.value;
		}
	}

	update { 
		slider.isActive.if({
			(mixer.notNil and: { mixer.preSends[index].notNil }).if({
				slider.value_(spec.unmap(mixer.preSends[index].level));
			}, {
				this.clearView;
			});
		});
	}

	updateMenu { 
		menu.items_(gui.menuItems);
		(mixer.notNil and: { mixer.preSends[index].notNil }).if({
			menu.value_(oldValue = mixer.preSends[index].outbus.index)
		}, {
			menu.value_(oldValue = gui.menuItems.size - 1);
		});
	}

	clearView { 
		this.updateMenu;
		slider.value_(0);
	}
	
	refresh { |bounds|
//"send gui refresh".debug;
		slider.bounds = this.getSliderBounds(bounds);
		menu.bounds = this.getMenuBounds(bounds);
		this.updateMenu;
		this.update;
	}

	free {
		menu.remove; slider.remove;
	}
}

MixerPostsendWidget : MixerPresendWidget {
	sendType { ^\post }
	doSliderAction { |view|
		mixer !? {
			mixer.postSends[index].tryPerform('level_', 
				spec.map(view.value), false)
		}
	}
	doMenuAction { |view|
		mixer !? {
			(menu.value != oldValue).if({
					// if old value is "none," create a postsend
				(oldValue == (menu.items.size-1)).if({
						// default level is 0
					"Making postsend".postln;
					MixerPostSend.new(mixer, menu.value, 0);
					slider.setProperty(\value, 0);
				}, {
						// if new value is "none," free the postsend
					(menu.value == (menu.items.size-1)).if({
						("Freeing postsend " ++ index).postln;
						(mixer.postSends[index]).free;
					}, {
						("Repatching send from bus " ++
							(mixer.postSends[index]).outbus.index ++ " to " ++ 
							menu.value).postln;
							// otherwise, just change the destination
						(mixer.postSends[index]).outbus = menu.value;
					});
				});
			});
				// save for next call
			oldValue = menu.value;
		}
	}

	update { 
		slider.isActive.if({
			(mixer.notNil and: { mixer.postSends[index].notNil }).if({
				slider.value_(spec.unmap(mixer.postSends[index].level));
			}, {
				this.clearView;
			});
		});
	}
	updateMenu { 
		menu.items_(gui.menuItems);
		(mixer.notNil and: { mixer.postSends[index].notNil }).if({
			menu.value_(oldValue = mixer.postSends[index].outbus.index)
		}, {
			menu.value_(oldValue = gui.menuItems.size - 1);
		});
	}
}

MixerPanWidget : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.slider.new(layout, bounds);
		this.restoreView;
		spec = \bipolar.asSpec;
	}
	doAction { |view|
		mixer.setControl(\pan, spec.map(view.value), false);
	}
	updateView { |value|
		view.value_(spec.unmap(value ?? { mixer.getControl(\pan) }));
	}
	clearView { 
		view.background_(Color.clear);
		view.value_(0.5);
	}
	restoreView { 
		view.background_(HiliteGradient(gui.color2, gui.color1, \h, 50, 0.5));
	}
	defaultValue { ^0.5 }
	updateKeys { ^\pan }
}

MixerLevelSlider : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.slider.new(layout, bounds);
		this.restoreView;
		spec = \amp.asSpec;
	}
	doAction { |view|
		mixer.setControl(\level, spec.map(view.value));
	}
	updateView { |value|
		view.value_(spec.unmap(value ?? { mixer.getControl(\level) }));
	}
	clearView { 
		view.background_(Color.clear);
		view.value_(0);
	}
	restoreView { 
		view.background_(Gradient(gui.color2, gui.color1, \v, 50));
	}
	updateKeys { ^\level }
}

MixerLevelSliderH : MixerLevelSlider {
	restoreView { 
		view.background_(Gradient(gui.color1, gui.color2, \h, 50));
	}
}

MixerLevelNumber : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.numberBox.new(layout, bounds)
			.font_(this.class.font)
			.align_(\center);
		spec = \amp.asSpec;
	}
	doAction { |view|
		mixer.setControl(\level, view.value.dbamp);
	}
	updateView { |value|
		view.value_((value ?? { mixer.getControl(\level) }).ampdb.round(0.001));
	}
	clearView { 
		view.value_(0);
	}
	updateKeys { ^\level }
}

MixerNameWidget : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.dragSink.new(layout, bounds)
			.font_(this.class.font)
			.align_(\center)
	}
	checkDoAction { |view|
		{	view.object.draggedIntoMixerGUI(gui);
		}.try({ |error|
			error.isKindOf(DoesNotUnderstandError).not.if({
				error.throw	// rethrow other errors
			}, {
				gui.refresh	// else, reset mixer name
			});
		});
	}
	updateView { |value|
		view.string_(value ?? { mixer.name });
	}
	clearView { 
		view.string_("inactive");
	}
	updateKeys { ^\name }
}

MixerOutbusWidget : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.popUpMenu.new(layout, bounds)
			.items_(gui.menuItems)
			.value_(gui.menuItems.size-1)
			.font_(this.class.font);
	}
	doAction {
		(mixer.notNil and: { view.value < (gui.menuItems.size - 1) }).if({
			mixer.outbus_(view.value);
		}, {
			this.updateView;
		});
	}
	updateView {
		view.value_(mixer.outbus.index);
	}
	restoreView {
		view.items_(gui.menuItems);
		
	}
	clearView {
		view.items_(["none"]);
		view.value_(0);
	}
	updateKeys { ^\outbus }
	defaultValue { ^gui.menuItems.size - 1 }
}

Mixer2DPanWidget : MixerWidgetBase {
	makeView { |layout, bounds|
		view = GUI.slider2D.new(layout, bounds);
		spec = \bipolar.asSpec;
	}
	doAction {
		mixer.setControl(\xpos, spec.map(view.x), updateGUI:false);
		mixer.setControl(\ypos, spec.map(view.y), updateGUI:false);
	}
	updateView {
		view.x = spec.unmap(mixer.getControl(\xpos));
		view.y = spec.unmap(mixer.getControl(\ypos));
	}
	clearView {
		view.x_(0.5).y_(0.5);
	}
	updateKeys { ^#[\xpos, \ypos] }
}
