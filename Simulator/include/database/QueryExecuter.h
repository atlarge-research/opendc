#pragma once
#include "Query.h"
#include "QueryResult.h"

#include <tuple>
#include <sqlite3.h>
#include <memory>
#include <vector>

namespace Database
{
	template<typename ...ReturnTypes>
	class QueryExecuter
	{
	public:
		/*
			Creates a prepared statement and initializes the query.
		*/
		explicit QueryExecuter(sqlite3* db) : database(db) {}

		/*
			Sets the query this executer will execute.
		*/
		QueryExecuter<ReturnTypes...>& setQuery(Query<ReturnTypes...> query)
		{
			this->query = std::make_unique<Query<ReturnTypes...>>(query);
			this->query->prepare(database);
			return *this;
		}

		/*
			Binds the given name-value pairs to the statement.
			Recursive case.
		*/
		template<typename BindType>
		QueryExecuter<ReturnTypes...>& bindParams(BindType locationValuePair, int depth = 1)
		{
			query->template bind<BindType>(locationValuePair, depth);
			return *this;
		}

		/*
			Binds the given name-value pairs to the statement.
			Recursive case.
		*/
		template<typename BindType, typename ...BindTypes>
		QueryExecuter<ReturnTypes...>& bindParams(BindType locationValuePair, BindTypes... locationValuePairs, int depth = 1)
		{
			query->template bind<BindType>(locationValuePair, depth);
			bindParams<BindTypes...>(locationValuePairs..., depth + 1);
			return *this;
		}

		/*
			Executes the query and returns a tuple with an entry for each type. Use this to recieve only the first result.
			Multiple calls, or calls when there are no rows in the db, can lead to exceptions.
			This is functionally equivalent to stepping and then calling getResult
		*/
		QueryResult<ReturnTypes...> executeOnce()
		{
			query->step();
			return QueryResult<ReturnTypes...>(getResult<ReturnTypes...>());
		}

		/*
			Steps the query.
		*/
		bool step()
		{
			return query->step();
		}

		/*
			Returns the result.
		*/
		QueryResult<ReturnTypes...> result()
		{
			return QueryResult<ReturnTypes...>(getResult<ReturnTypes...>());
		}

		/*
			Executes the query and returns a vector of tuples for each result.
		*/
		std::vector<QueryResult<ReturnTypes...>> execute(int limit = 0)
		{
			std::vector<QueryResult<ReturnTypes...>> result;
			
			// Return code
			bool more;

			int limiter = 0;

			do
			{
				// Execution of the statement
				more = query->step();

				if (!more || (limiter >= limit && limit > 0))
					break;

				limiter++;
				std::tuple<ReturnTypes...> row = getResult<ReturnTypes...>();
				result.emplace_back(row);

			} while (more);

			return result;
		}

		/*
			Resets the sqlite3 query object.
		*/
		QueryExecuter<ReturnTypes...>& reset()
		{
			query->reset();
			return *this;
		}

	private:
		/*
			Returns the results after executing the query.
			Base case.
		*/
		template<typename ReturnType>
		std::tuple<ReturnType> getResult(int depth = 0) 
		{ 
			return std::tuple<ReturnType>(query->template getResult<ReturnType>(depth));
		}

		/*
			Returns the results after executing the query.
			Recursive Case.
		*/
		template<typename FirstReturnType, typename SecondReturnType, typename ...OtherReturnTypes>
		std::tuple<FirstReturnType, SecondReturnType, OtherReturnTypes...> getResult(int depth = 0)
		{
			std::tuple<FirstReturnType> first = std::tuple<FirstReturnType>(query->template getResult<FirstReturnType>(depth));
			std::tuple<SecondReturnType, OtherReturnTypes...> rest = getResult<SecondReturnType, OtherReturnTypes...>(depth + 1);
			return std::tuple_cat(first, rest);
		}

		/*
			Returns an empty tuple for when there are no return values.
		*/
		template<typename ...NoTypes>
		std::tuple<> getResult(int depth = 0) const
		{
			return std::tuple<>();
		}

		std::unique_ptr<Query<ReturnTypes...>> query;
		sqlite3* database;
	};
}

