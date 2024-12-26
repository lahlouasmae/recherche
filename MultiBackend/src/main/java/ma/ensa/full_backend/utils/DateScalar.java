package ma.ensa.full_backend.utils;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateScalar {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static final GraphQLScalarType DATE = GraphQLScalarType.newScalar()
            .name("Date")
            .description("Custom scalar for handling dates in the format yyyy-MM-dd")
            .coercing(new Coercing<Date, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof Date) {
                        return DATE_FORMAT.format((Date) dataFetcherResult);
                    }
                    throw new CoercingSerializeException("Invalid value for Date");
                }

                @Override
                public Date parseValue(Object input) {
                    if (input instanceof String) {
                        try {
                            return DATE_FORMAT.parse((String) input);
                        } catch (ParseException e) {
                            throw new CoercingParseValueException("Invalid value for Date: " + input);
                        }
                    }
                    throw new CoercingParseValueException("Invalid value for Date: " + input);
                }

                @Override
                public Date parseLiteral(Object input) {
                    if (input instanceof StringValue) {
                        try {
                            return DATE_FORMAT.parse(((StringValue) input).getValue());
                        } catch (ParseException e) {
                            throw new CoercingParseLiteralException("Invalid value for Date literal");
                        }
                    }
                    throw new CoercingParseLiteralException("Invalid value for Date literal");
                }
            })
            .build();
}
