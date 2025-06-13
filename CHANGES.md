## Changes

Blade JIT allows us to re-assess a few things about Blade and introduce greater flexibility and security to the language
and runtime in general.

- `const` keyword for declaring constants.
- Everything except `nil` is now an object &mdash; even numbers and boolean.
- A new way for type validation: Formerly, to verify if a value is of a type, we use one of the `is_*` functions. For
  example, we have the built-in function `is_string`. This gave rise to too many built-in functions using up words that
  would have made good variable names and therefore easy to overwrite. With the new update to the language, we can do
  `string.get_class() == String`. Since `get_class()` is a method available of all objects, this is less easy to
  overwrite and thereby leads to a more secure code.
- Reinstated `try..catch..finally..` exception handling construct as the `catch..as..` introduced an insecure overhead
  to emulating the `finally..` behavior which itself is a great requirement for secure code.
- Variadic function and methods arguments must be assigned a name at the point of declaration and the automatically
  declared variable `__args__` is no longer declared. E.g. `def my_function(...my_arg)`. This allows for more
  flexibility and intentionality in variadic function handling.
- All objects now automatically declare the `to_string()` instance method and all classes have an automatic
  `to_string()` function if they fail to declare an override. For this reason, the `to_string()` built-in function is no
  longer available.
- String and List no longer have a `.length()` method but rather have a `.length` property. This property carries a
  restriction that they cannot be overwritten.
- Dictionary no longer exposes the `.length()` method. Instead, the `.size()` method has replaced it.
- All builtin methods can now also be accessed via their class. This removes ambiguity between methods and functions
  when overwritten by a program. For example, before, should a dictionary contain an object `length`, it automatically
  becomes impossible for us to get the length of that dictionary as `.length()` will return the new declaration. By
  allowing calling object methods directly from their class, one can still get the length of the dictionary by via the
  class by calling `Dictionary.size(my_dict)`.