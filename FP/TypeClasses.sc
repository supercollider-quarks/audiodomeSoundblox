/*
    FP Quark
    Copyright 2012 Miguel Negrão.

    FP Quark: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FP Quark is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FP Quark.  If not, see <http://www.gnu.org/licenses/>.

    It is possible to add more type instances by adding the functions
    directly to the dict from the initClass function of the class that
    one wants to make an instance of some type class.
*/
TypeClasses {

	classvar <>dict;

	*initClass {
        var metaRules;
		dict = IdentityDictionary.new;

		dict.put(Array,
			(
				'fmap': { |fa,f| fa.collect(f) },
				'bind' : { |fa,f| fa.collect(f).flatten },
				'pure' : { |a| [a] },
				'traverse' : { |f,as|
					var fclass;
					var fa = f.(as[0]);
					fclass = if( fa.class.isMetaClass ) {
						fa
					} {
						fa.class
					};
					as.reverse.inject( [].pure(fclass), { |ys,v|
						f.(v).fmap({ |z| { |zs| [z]++zs } }) <*> ys
					});
				}

			);
		);

        //Use startup for the meta rules ?
		metaRules = [
			//all Monads are Applicative functors
			[ ['bind'], ('apply' : { |f,fa| f >>= { |g| fa.fmap( g ) } } ) ]
		];

    	//change this to immutable once we have immutable collections
		metaRules.do{ |rule|
			dict.do{ |innerDict|
				//if the class is an instance of all the typeclasses declared in the rule
				//then add the payload
				if( rule[0].as(Set).isSubsetOf(innerDict.keys.as(Set)) ) {
					innerDict.putAll( rule[1] )
				}
			}
		}
	}

	*getSuperclassImplementation { |class, funcName|
		var g = { |class|
			if(class.notNil) {
				TypeClasses.dict.at(class) !? _.at(funcName) ?? { g.(class.superclass) }
			} {
				nil
			}
		};
		^g.(class)
	}

	*addInstance { |class, subdict|
		dict.put(class, subdict)
	}

}
