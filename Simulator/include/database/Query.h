#pragma once
#include <assert.h>
#include <sqlite3.h>

namespace Database
{

	namespace Specializations
	{
		template<typename ReturnType>
		ReturnType getResult(int location, sqlite3_stmt* statement);

		template<>
		inline int getResult<int>(int location, sqlite3_stmt* statement)
		{
			return sqlite3_column_int(statement, location);
		}

		template<>
		inline std::string getResult<std::string>(int location, sqlite3_stmt* statement)
		{
			return std::string(reinterpret_cast<const char*>(sqlite3_column_text(statement, location)));
		}

		template<typename ValueType>
		void bind(ValueType value, int location, sqlite3_stmt* statement);

		template<>
		inline void bind<int>(int value, int location, sqlite3_stmt* statement)
		{
			int rc = sqlite3_bind_int(statement, location, value);
			assert(rc == SQLITE_OK);
		}

		template<>
		inline void bind<float>(float value, int location, sqlite3_stmt* statement)
		{
			int rc = sqlite3_bind_double(statement, location, static_cast<double>(value));
			assert(rc == SQLITE_OK);
		}
	}



	template<typename ...ReturnTypes>
	class Query
	{
	public:
		explicit Query(std::string query) : statement(nullptr), content(query)
		{}

		/*
			Calls sqlite3_finalize on the statement.
		*/
		~Query()
		{
			int rc = sqlite3_finalize(statement);
			assert(rc == SQLITE_OK);
		}

		/*
			Calls sqlite3_prepare_v2 to prepare this query.
		*/
		void prepare(sqlite3* db)
		{
			// Preparation of the statement.
			int rc = sqlite3_prepare_v2(db, content.c_str(), static_cast<int>(content.size()), &statement, NULL);
			assert(rc == SQLITE_OK);
		}

		/*
			Steps the execution of this query once. Returns true if the return code is SQLITE_ROW.
		*/
		bool step() const
		{
			// Execution of the statement
			int rc = sqlite3_step(statement);
			if(rc == SQLITE_ROW)
				return true;
			if(rc == SQLITE_DONE)
				return false;

			assert(!"The return code of step was not SQLITE_ROW (100) or SQLITE_DONE (101)!");
			return false;
		}

		/*
			Resets this query back to its initial state.
		*/
		void reset() const
		{
			sqlite3_reset(statement);
		}

		/*
			A template for implementing the binding of values to parameters in the sqlite statement.
		*/
		template<typename ValueType>
		void bind(ValueType value, int location)
		{
			Specializations::bind<ValueType>(value, location, statement);
		}

		/**
		 * \brief Returns the result of ReturnType at the given location in the query result row.
		 * \tparam ReturnType The type of the entry in the row.
		 * \param location The index of the entry in the row.
		 * \return The result of the query at the given location.
		 */
		template<typename ReturnType>
		ReturnType getResult(int location)
		{
			return Specializations::getResult<ReturnType>(location, statement);
		}

	private:
		// The sqlite3 statement that corresponds to this query.
		sqlite3_stmt* statement;
		// The sql string that will be executed.
		std::string content;

	};

}
