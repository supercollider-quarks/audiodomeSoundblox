
/*
for use in MxInlet and MxOutlet objects

the adapters describe the functionality and capability of the object that they adapt for.

many objects may play audio on a Bus. an MxHasBus adapter expresses those types of objects and can be used to get that Bus

the strategy objects contain the connection implementations that connect one adapted object to another adapted object, where the Adapter is used to obtain the resource from the adapted objects.

eg. to fetch the Bus, get/set value, set action etc.

they do not hold any state or reference to specific objects

*/


AbsMxAdapter {}

AbsMxFuncAdapter : AbsMxAdapter {
	
	var <>func,>getServer,>getGroup;

	*new { arg thingGetter,getServer,getGroup;
		^super.new.func_(thingGetter).getServer_(getServer).getGroup_(getGroup)
	}	
	value { ^func.value }	
	server { ^getServer.value }
	group { ^getGroup.value }
}

MxHasBus : AbsMxFuncAdapter {}

MxHasJack : AbsMxFuncAdapter {}
MxHasKrJack : AbsMxFuncAdapter {}
MxHasStreamJack : AbsMxFuncAdapter {}

MxPlaysOnBus : AbsMxFuncAdapter {}
MxPlaysOnKrBus : AbsMxFuncAdapter {} 
MxListensToBus : AbsMxFuncAdapter {}
MxChannelInputAdapter : AbsMxFuncAdapter {}

MxSetter : AbsMxFuncAdapter {}
MxHasAction : AbsMxFuncAdapter {}
MxSendsValueOnChanged : AbsMxFuncAdapter {}
MxSendSelfOnChanged : AbsMxFuncAdapter {}
MxIsFrameRateDevice : AbsMxFuncAdapter {}
MxIsStream : AbsMxFuncAdapter {}

