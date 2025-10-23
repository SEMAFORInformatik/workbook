package ch.semafor.intens.ws.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import ch.semafor.gendas.model.PropertyType;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.gendas.search.SearchIn;
import ch.semafor.gendas.search.SearchInterval;
import ch.semafor.gendas.search.SearchOp;

public class SearchFactory {

	/**
	 * create search object based on cond. If
	 * cond contains (lower,upper): a corresponding search object
	 * is created
	 * @param cond search conditon
	 * @param type of expression
	 * @param ignorecase ignore case if true
	 * @return search operator
	 * @throws ClassNotFoundException class not found
	 * @throws SecurityException access denied
	 * @throws NoSuchMethodException no such method
	 * @throws InvocationTargetException cannot invoke
	 * @throws IllegalArgumentException wrong arg
	 * @throws IllegalAccessException illegal access
	 * @throws InstantiationException cannot instantiate
	 */
	static public SearchOp createSearchObject(String cond,
			PropertyType.Type type, Boolean ignorecase)
			throws ClassNotFoundException, NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Constructor<?> con = null;
		switch( type ){
		case REAL:
		case STRING:
		case INTEGER:
		case LONG:
			con = Class.forName(type.getName()).getConstructor(String.class);
			break;
		case CDATA:
			con = Class.forName("java.util.String").getConstructor(String.class);
			break;
		default:
			// Must be date which is constructed by date formatter
		}
		Integer closing_char_index = 0;
		try {
			cond = cond.trim(); // Remove white spaces
			if (cond.startsWith("(") || cond.startsWith("[")) {
				// interval has to be closed with ) or ]
				// check if ')' 
				closing_char_index = cond.indexOf(")");
				// if no ) was found check if ] exist
				if( closing_char_index <= 0) {
					closing_char_index = cond.indexOf("]");
				}
				// if no closing char exist throw an IllegalArgumentException
				if( closing_char_index <= 0) {
					throw new IllegalArgumentException("Interval search must be closed by ')' or ']' character!");
				}
				// Create cond from start to closing_char (+1 because the closing char is needed!)
				cond = cond.substring(0, closing_char_index + 1);
				
				String[] vals = cond.substring(1, cond.length() - 1).split(",");
				Object lower=null;
				Object upper=null;
				if (con != null) {
				  if( !vals[0].isEmpty())
				    lower = con.newInstance(vals[0]);
					if( vals.length>1)
					  upper = con.newInstance(vals[1]);
				} else { // must be a date
					// accept date format rfc 822 and xmldate
					if( vals[0] != null)
						lower = DateTimeFormatter.convert(vals[0]);
					if( vals.length>1)
					  upper = DateTimeFormatter.convert(vals[1]);
				}
				SearchInterval.Bounds bounds=null;
				if( cond.startsWith("(") && cond.endsWith("]")) {
					bounds=SearchInterval.Bounds.LeftOpen;
				} else if (cond.startsWith("[") && cond.endsWith("]")) {
					bounds=SearchInterval.Bounds.Bounded;
				} else if (cond.startsWith("[") && cond.endsWith(")")) {
					bounds=SearchInterval.Bounds.RightOpen;
				} else {
					bounds=SearchInterval.Bounds.Open;
				}
				return new SearchInterval<Object>(lower, upper,	bounds);
			}
			// Search In
			if (cond.startsWith("{") && cond.endsWith("}")) {
				final String[] strings = cond.substring(1, cond.length() - 1).split(",");
				switch( type ){
				case CDATA:
				case STRING:
					return new SearchIn<Object>(strings);
				case REAL:
					Double[] reals = new Double[strings.length];
					for (int i=0; i < strings.length; i++) {
						if( strings[i] != null && !strings[i].isEmpty())
							reals[i] = Double.parseDouble(strings[i]);
				    }
					return new SearchIn<Object>(reals);
				case INTEGER:
					Integer[] ints = new Integer[strings.length];
					for (int i=0; i < strings.length; i++) {
						if( strings[i] != null && !strings[i].isEmpty())
							ints[i] = Integer.parseInt(strings[i]);
				    }
					return new SearchIn<Object>(ints);
				case LONG:
					Long[] longs = new Long[strings.length];
					for (int i=0; i < strings.length; i++) {
						if( strings[i] != null && !strings[i].isEmpty())
							longs[i] = Long.parseLong(strings[i]);
				    }
					return new SearchIn<Object>(longs);
				default:
				}
				return new SearchIn<Object>(strings);
			}
			if (con != null) {
				if (type==PropertyType.Type.STRING) {
					return new SearchEq<String>(cond,ignorecase);
				}
				return new SearchEq<Object>(con.newInstance(cond));
			}
			// must be a date
			return new SearchEq<Date>(DateTimeFormatter.convert(cond));

		} catch (IllegalArgumentException pe) {
			throw new IllegalArgumentException("invalid date format " + cond);
		}
	}
}
