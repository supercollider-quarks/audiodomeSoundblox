// wslib 2006
// slider based on blackrain's knob

SmoothSlider : RoundView {
	var <>color, <value, <>step, hit, <>keystep, <>mode, isCentered = false;
	var <border = 0, <baseWidth = 1, <extrude = false, <knobBorderScale = 2;
	var <knobSize = 0.25, hitValue = 0;
	var <orientation = \v;
	var <thumbSize = 0; // compatible with old sliders
	var <focusColor;
	
	var <string, <font, <align, <stringOrientation = \h, <stringAlignToKnob = false;
	
	var <>shift_scale = 100.0, <>ctrl_scale = 10.0, <>alt_scale = 0.1;
	
	*viewClass { ^SCUserView }
	
	init { arg parent, bounds;
		bounds = bounds.asRect;
		if( bounds.width > bounds.height )
			{ orientation = \h };
					
		super.init( parent, bounds );
				
		mode = \jump;  // \jump or \move
		keystep = 0.01;
		step = 0.01;
		value = 0.0;
		
		// background, hilightColor, borderColor, knobColor, stringColor
		color = [Color.gray(0.5, 0.5), Color.blue.alpha_(0.5), Color.white.alpha_(0.5),
			Color.black.alpha_(0.7), Color.black ];
	}
	
	sliderBounds {
		var realKnobSize, drawBounds, rect;
		
		rect = this.drawBounds;
				
		drawBounds = rect.insetBy( border, border );
				
		if( orientation == \h )
				{  drawBounds = Rect( drawBounds.top, drawBounds.left, 
					drawBounds.height, drawBounds.width ); 
				};
				
		realKnobSize = (knobSize * drawBounds.width)
					.max( thumbSize ).min( drawBounds.height );
		
		
		^drawBounds.insetBy( 0, realKnobSize / 2 );
		}
		
	focusColor_ { |newColor| focusColor = newColor; this.parent.refresh; }
		
	knobColor { ^color[3] }
	knobColor_ { |newColor| color[3] = newColor; this.refresh; }
	
	background { ^color[0] }
	background_ { |newColor| color[0] = newColor; this.refresh; }
	
	borderColor { ^color[2] }
	borderColor_ { |newColor| color[2] = newColor; this.refresh; }
	
	border_ { |newBorder| border = newBorder; this.refresh; }
	
	extrude_ { |bool| extrude = bool; this.refresh; }
	
	knobBorderScale_ { |value| knobBorderScale = value; this.refresh; }
	
	baseWidth_ { |newBaseWidth| baseWidth = newBaseWidth; this.refresh; }
	
	hilightColor { ^color[1] }
	hilightColor_ { |newColor| color[1] = newColor; this.refresh; }
	
	hiliteColor { ^color[1] } // slang but compatible
	hiliteColor_ { |newColor| color[1] = newColor; this.refresh; }
	
	thumbSize_ { |newSize = 0| thumbSize = newSize; this.refresh; }
	
	relThumbSize { if( orientation == \h ) 
		{ ^thumbSize / this.bounds.width  }
		{ ^thumbSize / this.bounds.height };
		}
	
	relThumbSize_ { |newSize = 0| if( orientation == \h ) 
		{ thumbSize = newSize * this.bounds.width }
		{ thumbSize = newSize * this.bounds.height };
		this.refresh;
		}
		
	absKnobSize {  if( orientation == \h ) 
		{ ^knobSize * this.bounds.height  }
		{ ^knobSize * this.bounds.width };
	 }
	 
	stringColor { ^color[4] } // slang but compatible
	stringColor_ { |newColor| 
		if( color.size > 4 )
			{ color[4] = newColor; }
			{ color = color ++ [newColor] };
		this.refresh;
		 }
		 
	font_ { |newFont| font = newFont; this.refresh; }
	string_ { |newString| string = newString; this.refresh; }
	align_ { |newAlign| align = newAlign; this.refresh; }
	
	stringAlignToKnob_ { |bool| stringAlignToKnob = (bool == true); this.refresh; }
	
	draw {
		var startAngle, arcAngle, size, widthDiv2, aw;
		var knobPosition, realKnobSize;
		var rect, drawBounds, radius;
		var baseRect, knobRect;
		var center, strOri;
		
		var bnds; // used with string
		
		Pen.use {
			
			rect = this.drawBounds;
				
			drawBounds = rect.insetBy( border, border );
			
			if( orientation == \h )
				{  drawBounds = Rect( drawBounds.top, drawBounds.left, 
					drawBounds.height, drawBounds.width );
					
				   // baseRect = drawBounds.insetBy( (1-baseWidth) * (drawBounds.width/2), 0 );
				   
				   Pen.rotate( 0.5pi, (rect.left + rect.right) / 2, 
				   					 rect.left + (rect.width / 2)  );
				};
			
			baseRect = drawBounds.insetBy( (1-baseWidth) * (drawBounds.width/2), 0 );
			
			size = drawBounds.width;
			widthDiv2 = drawBounds.width * 0.5;
					
			realKnobSize = (knobSize * drawBounds.width)
					.max( thumbSize ).min( drawBounds.height );
			radius = (knobSize * drawBounds.width) / 2;
			knobPosition = drawBounds.top + ( realKnobSize / 2 )
						+ ( (drawBounds.height - realKnobSize) * (1- value).max(0).min(1));
			
			if( this.hasFocus ) // rounded focus rect
				{
				Pen.use({
					Pen.color = focusColor ?? { Color.gray(0.2).alpha_(0.8) };
					Pen.width = 2;
					Pen.roundedRect( baseRect.insetBy(-2 - border,-2 - border), 
						(radius.min( baseRect.width/2) + 1) + border );
					Pen.stroke;
					});
				};
				
			Pen.use{	
			color[0] !? { // base / background
				//Pen.fillColor = color[0];
				Pen.roundedRect( baseRect, radius.min( baseRect.width/2) );
				color[0].fill( baseRect );
				};
			
			if( backgroundImage.notNil )
				{ 
				Pen.roundedRect( baseRect, radius.min( baseRect.width/2) );
				backgroundImage[0].fill( baseRect, *backgroundImage[1..] );
				}
			};
			
			Pen.use{
			color[2] !? { // // border
				if( border > 0 )
					{ 
					
					  if( color[2].notNil && { color[2] != Color.clear } )
					  	{	 Pen.strokeColor = color[2];
						  Pen.width = border;
						  Pen.roundedRect( baseRect.insetBy( border/(-2), border/(-2) ), 
						  	radius.min( baseRect.width/2) + (border/2) ).stroke;
						  };
					  if( extrude )
					  	{ 
					  	Pen.use{	
						  	  Pen.rotate( (h: -0.5pi, v: 0 )[ orientation ],
					   				(rect.left + rect.right) / 2, 
					   				rect.left  + (rect.width / 2)  );
					   		
						  	  Pen.extrudedRect( 
						  	  	baseRect.rotate((h: 0.5pi, v: 0 )[ orientation ],
					   				(rect.left + rect.right) / 2, 
					   				rect.left  + (rect.width / 2))
					   					.insetBy( border.neg, border.neg ), 
						  		(if( radius == 0 ) 
						  			{ radius } { radius + border }).min( baseRect.width/2 ),
						  		border, 
						  		inverse: true )
						  	}
					  	};
					};
				};
				};
			
			Pen.use{	
			
			color[1] !? { 
				//color[1].set; // hilight
				if( isCentered )
				{
				Pen.roundedRect( Rect.fromPoints( 
						baseRect.left@
							((knobPosition - (realKnobSize / 2))
								.min( baseRect.center.y ) ),
						baseRect.right@
							((knobPosition + (realKnobSize / 2))
								.max( baseRect.center.y  ) ))
						
					, radius ); //.fill;
				color[1].fill( baseRect );
				}
				{
				Pen.roundedRect( Rect.fromPoints( 
						baseRect.left@(knobPosition - (realKnobSize / 2)),
						baseRect.right@baseRect.bottom ), radius.min( baseRect.width/2) );
				
				color[1].fill( baseRect );
				};
				};
				
				};
				
			Pen.use{
	
			color[3] !? {	 
				knobRect =  Rect.fromPoints(
					Point( drawBounds.left, 
						( knobPosition - (realKnobSize / 2) ) ),
					Point( drawBounds.right, knobPosition + (realKnobSize / 2) ) );

				Pen.roundedRect( knobRect, radius );//.fill; 
				
				color[3].fill( knobRect ); // requires extGradient-fill.sc methods
				
				 if( extrude && ( knobRect.height >= border ) )
					  	{ 
					  	Pen.use{	
						  	  Pen.rotate( (h: -0.5pi, v: 0 )[ orientation ],
					   				(rect.left + rect.right) / 2, 
					   				rect.left  + (rect.width / 2)  );
					   		
						  	  Pen.extrudedRect( 
						  	  	knobRect.rotate((h: 0.5pi, v: 0 )[ orientation ],
					   				(rect.left + rect.right) / 2, 
					   				rect.left  + (rect.width / 2)), 
						  		radius.max( border ), border * knobBorderScale)
						  	}
					  	};
				};
				
				};
			
			string !? {
				
				
				if( stringAlignToKnob )
					{ bnds = knobRect ?? { Rect.fromPoints(
						Point( drawBounds.left, 
							( knobPosition - (realKnobSize / 2) ) ),
						Point( drawBounds.right, knobPosition + (realKnobSize / 2) ) );  }; }
					{ bnds = drawBounds };
				
				stringOrientation = stringOrientation ? \h;
								
				Pen.use{	
					
					center = drawBounds.center;
					
					strOri = (h: 0pi, v: 0.5pi, u: -0.5pi, d: 0.5pi, up: -0.5pi, down: 0.5pi )
							[stringOrientation] ? stringOrientation;
					
					strOri = strOri + (h: -0.5pi, v: 0)[ orientation ];
					
					if( strOri != 0 )
					{ Pen.rotate( strOri, center.x, center.y );
					 bnds = bnds.rotate( strOri.neg, center.x, center.y );
					};
						 		 
					font !? { Pen.font = font };
					Pen.color = color[4] ?? { Color.black; };
					string = string.asString;
					
					switch( align ? \center,
						\center, { Pen.stringCenteredIn( string, bnds ) },
						\middle, { Pen.stringCenteredIn( string, bnds ) },
						\left, { Pen.stringLeftJustIn( string, bnds ) },
						\right, { Pen.stringRightJustIn( string, bnds ) } );
					
					font !? { Pen.font = nil; };
					};
				};
			
			if( enabled.not )
				{
				Pen.use {
					Pen.fillColor = Color.white.alpha_(0.5);
					Pen.roundedRect( 
						baseRect.insetBy( border.neg, border.neg ), 
						radius.min( baseRect.width/2) ).fill;
					};
				};
		
			};
		}
		
	getScale { |modifiers| 
		^case
			{ modifiers & 131072 == 131072 } { shift_scale }
			{ modifiers & 262144 == 262144 } { ctrl_scale }
			{ modifiers & 524288 == 524288 } { alt_scale }
			{ 1 };
	}
	
	pixelStep { 
		var bounds = this.sliderBounds; 
		^(bounds.width.max(bounds.height) - this.thumbSize).reciprocal
	}
	

	mouseDown { arg x, y, modifiers, buttonNumber, clickCount;
		if( enabled ) {	
			hit = Point(x,y);
				
			hitValue = value;
			this.mouseMove(x, y, modifiers);
		};
		
	}
	
	mouseMove { arg x, y, modifiers;
		var pt, angle, inc = 0;
		var bounds;
		if( enabled ) {	
			bounds = this.drawBounds;
			if (modifiers != 1048576, { // we are not dragging out - apple key
				case
					{ mode == \move } {
						if( orientation == \v )
							{ if( thumbSize < bounds.height )
								{ value = ( hitValue + ( 
										( (hit.y - y) / this.sliderBounds.height )  
											* this.getScale( modifiers ) ) )
								.clip( 0.0,1.0 ); }; }
							{ if( thumbSize < bounds.width )
								{ value = ( hitValue + ( 
										( (x - hit.x) / this.sliderBounds.height  ) 
										* this.getScale( modifiers ) ) )
								.clip( 0.0,1.0 ); } };
								
						//hit = Point(x,y);
						action.value(this, x, y, modifiers);
						this.refresh;
					}
					{ mode == \jump } {
						if( orientation == \v )
							{ 
							if( thumbSize < bounds.height )
								{ value = ( 1 - ((y - (bounds.top + (
										( knobSize * bounds.width )
										.max( thumbSize.min( bounds.height ) ) / 2))) / 
									(bounds.height - 
										(knobSize * bounds.width )
										.max( thumbSize )  ))
									).clip( 0.0,1.0 );
									};
							 }
							{ if( thumbSize < bounds.width )
								{ value = ((x - (bounds.left + (
									( knobSize * bounds.height )
									.max( thumbSize.min( bounds.width ) ) / 2))) / 
								(bounds.width - (knobSize * bounds.height )
									.max( thumbSize.min( bounds.width ) ) ))
									.clip(0.0,1.0); };
							};
								
						//hit = Point(x,y);
						action.value(this, x, y, modifiers);
						this.refresh;
					}
			});
		};
	}

	value_ { arg val;
		value = val.clip(0.0, 1.0);
		this.refresh;
	}

	valueAction_ { arg val;
		value = val.clip(0.0, 1.0);
		action.value(this);
		this.refresh;
	}
	
	safeValue_ {  // prevent crash when window is closed
		 arg val;
		value = val.clip(0.0, 1.0);
		if( parent.notNil && { parent.findWindow.dataptr.notNil } )
			{ this.refresh; }
		}
	

	centered_ { arg bool;
		isCentered = bool;
		this.refresh;
	}
	
	centered {
		^isCentered
	}
	
	orientation_ { |newOrientation| 
		if( stringOrientation == orientation ) { stringOrientation = newOrientation };
		orientation = newOrientation ? orientation; this.refresh;
		
		 }
		 
	stringOrientation_ { |newOrientation| // resets if nil
		stringOrientation = newOrientation ? orientation;
		this.refresh;
		}
	
	knobSize_ { |newSize| knobSize = newSize ? knobSize; this.refresh; }
	
	increment { |zoom=1| ^this.valueAction = 
		( this.value + (max(this.step, this.pixelStep) * zoom) ).min(1); }
	decrement { |zoom=1| ^this.valueAction = 
		( this.value - (max(this.step, this.pixelStep) * zoom) ).max(0); }
	

	keyDown { arg char, modifiers, unicode,keycode;
		var zoom = this.getScale(modifiers); 
		
		// standard keydown
		if (char == $r, { this.valueAction = 1.0.rand; });
		if (char == $n, { this.valueAction = 0.0; });
		if (char == $x, { this.valueAction = 1.0; });
		if (char == $c, { this.valueAction = 0.5; });
		if (char == $], { this.increment(zoom); ^this });
		if (char == $[, { this.decrement(zoom); ^this });
		if (unicode == 16rF700, { this.increment(zoom); ^this });
		if (unicode == 16rF703, { this.increment(zoom); ^this });
		if (unicode == 16rF701, { this.decrement(zoom); ^this });
		if (unicode == 16rF702, { this.decrement(zoom); ^this });
		
		^nil;
		
	}

	defaultReceiveDrag {
		this.valueAction_(SCView.currentDrag);
	}
	defaultGetDrag { 
		^value
	}
	defaultCanReceiveDrag {
		^currentDrag.isFloat;
	}
}


