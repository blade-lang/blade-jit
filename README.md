# Blade-JIT

A JIT compiler for the Blade programming language.

### Introduces:

- `const` keyword for declaring constants.
- Everything except `nil` is now an object &mdash; even numbers and boolean.
- A new way for type validation: Formerly, to verify if a value is of a type, we use one of the `is_*` functions. For example, we have the built-in function `is_string`. This gave rise to too many built-in functions using up words that would have made good variable names and therefore easy to overwrite. With the new update to the language, we can do `string.get_class() == String`. Since `get_class()` is a method available of all objects, this is less easy to overwrite and thereby leads to a more secure code.
- Reinstated `try..catch..finally..` exception handling construct as the `catch..as..` introduced an insecure overhead to emulating the `finally..` behavior which itself is a great requirement for secure code.
- Variadic function and methods arguments must be assigned a name at the point of declaration and the automatically declared variable `__args__` is no longer declared. E.g. `def my_function(...my_arg)`. This allows for more flexibility and intentionality in variadic function handling.
- All objects now automatically declare the `to_string()` instance method and all classes have an automatic `to_string()` function if they fail to declare an override. For this reason, the `to_string()` built-in function is no longer available.