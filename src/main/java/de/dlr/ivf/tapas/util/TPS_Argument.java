package de.dlr.ivf.tapas.util;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Class for runtime arguments pasted via the command line
 * @author hein_mh
 *
 */
public class TPS_Argument {

	/**
	 * Inner Class to specify a type of argument and its default values.
	 * @author hein_mh
	 *
	 * @param <T> The classtyype for this argument
	 */
	
	public static class TPS_ArgumentType<T> {
		private Class<T> clazz;
		private boolean required;
		private String name;
		private T defaultObject;

		/**
		 * Constructor with name class and default object. If default object is set to null the Argument is nor necessarily required.
		 * @param name Name for this argument
		 * @param clazz Classtype
		 * @param defaultObject Defaut object, set to null if not required
		 */
		public TPS_ArgumentType(String name, Class<T> clazz, T defaultObject) {
			this.clazz = clazz;
			this.required = defaultObject == null;
			this.name = name;
			this.defaultObject = defaultObject;
		}

		/**
		 * Constructor with name class and default object. 
		 * @param name Name for this argument
		 * @param clazz Classtype
		 */
		public TPS_ArgumentType(String name, Class<T> clazz) {
			this(name, clazz, null);
		}

		@Override
		public String toString() {
			return "[name='" + name + "', class=" + clazz.getName() + ", required=" + required + "]";
		}

	}

	/**
	 * Method to check a set of arguments pated by a String array for values and a Collection of expected TPS_ArgumentType 
	 * @param args the command line parameters provided
	 * @param types the types needed
	 * @return An array of correctly initialised arguments with values 
	 */
	public static Object[] checkArguments(String[] args, Collection<TPS_ArgumentType<?>> types) {
		TPS_ArgumentType<?>[] array = types.toArray(new TPS_ArgumentType[0]);
		return checkArguments(args, array);
	}

	/**
	 * Method to check a set of arguments pasted by a String array for values and an array of expected TPS_ArgumentType
	 * @param args the command line parameters provided
	 * @param types the types needed
	 * @return An array of correctly initialised arguments with values
	 * @throws IllegalArgumentException if given and necessary arguments does not match
	 */
	@SuppressWarnings("unchecked")
	public static Object[] checkArguments(String[] args, TPS_ArgumentType<?>... types) throws IllegalArgumentException{
		if (types.length > args.length) {
			throw new IllegalArgumentException("given arguments and necessary arguments have different sizes " +
					"-> |args|=" + args.length + ", |classes|=" + types.length);
		}
		Object[] array = new Object[types.length];

		for (int i = 0; i < array.length; i++) {
			if (args[i] == null || "null".equals(args[i])) {
				if (types[i].required)
					throw new IllegalArgumentException("Required argument " + types[i] + " not set at position " + i);
				array[i] = types[i].defaultObject;
			} else if (String.class.equals(types[i].clazz)) {
				array[i] = args[i];
			} else if (Boolean.class.equals(types[i].clazz)) {
				if ("true".equalsIgnoreCase(args[i]) || "false".equalsIgnoreCase(args[i])) {
					array[i] = Boolean.parseBoolean(args[i]);
				} else {
					throw new IllegalArgumentException("Value '" + args[i] + "' for argument " + types[i] + " at position " + i
							+ " is no type of " + types[i].clazz.getSimpleName());
				}
			} else if (Integer.class.equals(types[i].clazz)) {
				try {
					array[i] = Integer.parseInt(args[i]);
				} catch (Exception e) {
					throw new IllegalArgumentException("Value '" + args[i] + "' for argument " + types[i] + " at position " + i
							+ " is no type of " + types[i].clazz.getSimpleName(), e);
				}
			} else if (Long.class.equals(types[i].clazz)) {
				try {
					array[i] = Long.parseLong(args[i]);
				} catch (Exception e) {
					throw new IllegalArgumentException("Value '" + args[i] + "' for argument " + types[i] + " at position " + i
							+ " is no type of " + types[i].clazz.getSimpleName(), e);
				}
			} else if (Float.class.equals(types[i].clazz)) {
				try {
					array[i] = Float.parseFloat(args[i]);
				} catch (Exception e) {
					throw new IllegalArgumentException("Value '" + args[i] + "' for argument " + types[i] + " at position " + i
							+ " is no type of " + types[i].clazz.getSimpleName(), e);
				}
			} else if (Double.class.equals(types[i].clazz)) {
				try {
					array[i] = Double.parseDouble(args[i]);
				} catch (Exception e) {
					throw new IllegalArgumentException("Value '" + args[i] + "' for argument " + types[i] + " at position " + i
							+ " is no type of " + types[i].clazz.getSimpleName(), e);
				}
			} else if (File.class.equals(types[i].clazz)) {
				File file = new File(args[i]);
				if (!file.exists()) {
					throw new IllegalArgumentException("File '" + file + "' for argument " + types[i] + " at position " + i
							+ " does not exist");
				} else if (!file.canRead()) {
					throw new IllegalArgumentException("Can't read from file '" + file + "' for argument " + types[i]
							+ " at position " + i);
				}
				array[i] = file;
			} else {
				try {
					@SuppressWarnings("rawtypes")
					EnumSet<?> enumSet = EnumSet.allOf((Class<Enum>) types[i].clazz);
					StringBuilder sb = new StringBuilder("[");
					for (Enum<?> e : enumSet) {
						if (e.name().equals(args[i])) {
							array[i] = e;
							break;
						}
						sb.append(e.name() + ",");
					}
					if (array[i] == null) {
						sb.setCharAt(sb.length() - 1, ']');
						throw new RuntimeException("Enum value '" + args[i] + "' for argument " + types[i] + " at position " + i
								+ " not found in " + sb.toString());
					}
				} catch (Exception e) {
					throw new RuntimeException("No procedure for class of argument " + types[i] + " defined", e);
				}
			}
		}
		return array;
	}
}
