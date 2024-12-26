package ma.ensa.full_backend.utils;

import graphql.language.IntValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

public class LongScalar {

    public static final GraphQLScalarType LONG = GraphQLScalarType.newScalar()
            .name("Long")
            .description("Custom scalar for handling Java Long values")
            .coercing(new Coercing<Long, Long>() {
                @Override
                public Long serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof Long) {
                        return (Long) dataFetcherResult;
                    }
                    throw new CoercingSerializeException("Invalid value for Long: " + dataFetcherResult);
                }

                @Override
                public Long parseValue(Object input) {
                    if (input instanceof Number) {
                        return ((Number) input).longValue();
                    }
                    throw new CoercingParseValueException("Invalid value for Long: " + input);
                }

                @Override
                public Long parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        return ((IntValue) input).getValue().longValue();
                    }
                    throw new CoercingParseLiteralException("Invalid literal value for Long");
                }
            })
            .build();
}
