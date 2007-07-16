// (c) 2007 Dan Stowell
// Free to use under the GPL.
// See the Help file for [ProcessTools] to see what these do...

+ String {

unixCmdInferPID { |action|
	var cmdname, pipe, line, lines, prepids, postpids, diff, pid;
	Task({
		// cmdname is the command name we'll be monitoring
		cmdname = this.replace("\\ ", "").split($ ).first.basename;
		
		// List processes before we launch
		lines = ("ps -xc -o \"pid command\" | grep" + cmdname + "| sed 's/" ++ cmdname ++ "//; s/ //g'").unixCmdGetStdOut;
		prepids = if(lines.isNil, [], {lines.split($\n).collect(_.asInteger)});
		//("PIDS pre:  " + prepids).postln;
		
		// Run the cmd! NB use .unixCmd because we don't want to wait for a result (as would .systemCmd).
		this.unixCmd;

		0.1.wait;
		
		// List processes after we launch
		lines = ("ps -xc -o \"pid command\" | grep" + cmdname + "| sed 's/" ++ cmdname ++ "//; s/ //g'").unixCmdGetStdOut;
		postpids = if(lines.isNil, [], {lines.split($\n).collect(_.asInteger)});
		//("PIDS post: " + postpids).postln;
		
		// Can we spot a single addition?
		diff = difference(postpids, prepids).select(_ > 0);
		if(diff.size != 1, {
			("String.unixCmdInferPID - unable to be sure of the " ++ cmdname ++ " PID.").warn;
			("Compared:" + prepids.asCompileString + "against" + postpids.asCompileString + "with result" + diff.asCompileString).postln;
			pid = nil;
		}, {
			pid = diff[0];
		});
		
		action.value(pid);
		
	}).play(AppClock);
} // End .unixCmdInferPID

unixCmdThen { |action, checkevery=0.3|
	this.unixCmdInferPID({|pid|
		if(pid.isNil, {
			("String:unixCmdThen - could not infer PID, therefore couldn't wait until done!").error;
		}, {
			Task({
				checkevery.wait;
				while({ pid.isPIDRunning }, { checkevery.wait });
				action.value(pid);
			}).play(AppClock);
		});
	});
} // End .unixCmdThen

unixCmdGetStdOut {
	var pipe, lines, line;

	pipe = Pipe.new(this, "r");
	lines = "";
	line = pipe.getLine;
	while({line.notNil}, {lines = lines ++ line ++ "\n"; line = pipe.getLine; });
	pipe.close;
	
	^lines;
}

} // End String


//////////////////////////////////////////////////////////////////////////////////////////////////////////////

+ Integer {

isPIDRunning {
	^("ps -p" ++ this).unixCmdGetStdOut.contains(this.asString);
} // End isPIDRunning

} // End Integer
