#pragma once
#include "modeling\TypeIndex.h"

#include <gtest\gtest.h>

TEST(TypeIndexTest, SingleType)
{
	size_t index = indexOfType<int, int>();
	ASSERT_EQ(index, 0);
}

TEST(TypeIndexTest, MultipleTypesFirst)
{
	size_t index = indexOfType<int, int, std::string, bool>();
	ASSERT_EQ(index, 0);
}

TEST(TypeIndexTest, MultipleTypesMiddle)
{
	size_t index = indexOfType<std::string, int, std::string, bool>();
	ASSERT_EQ(index, 1);
}

TEST(TypeIndexTest, MultipleTypesLast)
{
	size_t index = indexOfType<bool, int, std::string, bool>();
	ASSERT_EQ(index, 2);
}