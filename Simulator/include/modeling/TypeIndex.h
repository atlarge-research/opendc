#pragma once
template<typename T, typename... Ts> struct indexOfType;

template <typename T, typename... Ts>
struct indexOfType<T, T, Ts...> : std::integral_constant<std::size_t, 0> {};

template <typename T, typename Tail, typename... Ts>
struct indexOfType<T, Tail, Ts...> : std::integral_constant<std::size_t, 1 + indexOfType<T, Ts...>::value> {};

