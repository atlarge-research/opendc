#pragma once
#include <tuple>

namespace Database
{
	template<class ...ReturnTypes>
	class QueryResult
	{
	public:
		explicit QueryResult(ReturnTypes... returnValues) : values(returnValues...) {}
		explicit QueryResult(std::tuple<ReturnTypes...> returnValues) : values(returnValues) {}

		/*
			Returns the item at the given index. 
			ReturnType must be the same as the type of the item at position Index in the tuple.
		*/
		template<class ReturnType, int Index>
		ReturnType get()
		{
			return std::get<Index>(values);
		}
		
		std::tuple<ReturnTypes...> values;
	};
}
