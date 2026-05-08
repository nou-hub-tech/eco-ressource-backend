package com.marketplace.backend.entity.workspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class WorkspaceJsonConverters {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  private WorkspaceJsonConverters() {}

  private abstract static class JsonAttributeConverter<T> implements AttributeConverter<T, String> {

    private final TypeReference<T> typeReference;

    protected JsonAttributeConverter(TypeReference<T> typeReference) {
      this.typeReference = typeReference;
    }

    protected abstract T emptyValue();

    @Override
    public String convertToDatabaseColumn(T attribute) {
      T safeValue = attribute == null ? emptyValue() : attribute;
      try {
        return MAPPER.writeValueAsString(safeValue);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Unable to serialize workspace json field", e);
      }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
      if (dbData == null || dbData.isBlank()) {
        return emptyValue();
      }

      try {
        return MAPPER.readValue(dbData, typeReference);
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to deserialize workspace json field", e);
      }
    }
  }

  @Converter
  public static class StringListJsonConverter extends JsonAttributeConverter<List<String>> {

    public StringListJsonConverter() {
      super(new TypeReference<>() {});
    }

    @Override
    protected List<String> emptyValue() {
      return new ArrayList<>();
    }
  }

  @Converter
  public static class DoubleListJsonConverter extends JsonAttributeConverter<List<Double>> {

    public DoubleListJsonConverter() {
      super(new TypeReference<>() {});
    }

    @Override
    protected List<Double> emptyValue() {
      return new ArrayList<>();
    }
  }

  @Converter
  public static class IntegerListJsonConverter extends JsonAttributeConverter<List<Integer>> {

    public IntegerListJsonConverter() {
      super(new TypeReference<>() {});
    }

    @Override
    protected List<Integer> emptyValue() {
      return new ArrayList<>();
    }
  }

  @Converter
  public static class IntegerMatrixJsonConverter
      extends JsonAttributeConverter<List<List<Integer>>> {

    public IntegerMatrixJsonConverter() {
      super(new TypeReference<>() {});
    }

    @Override
    protected List<List<Integer>> emptyValue() {
      return new ArrayList<>();
    }
  }

  @Converter
  public static class OrderLineItemListJsonConverter
      extends JsonAttributeConverter<List<WorkspacePayloads.OrderLineItem>> {

    public OrderLineItemListJsonConverter() {
      super(new TypeReference<>() {});
    }

    @Override
    protected List<WorkspacePayloads.OrderLineItem> emptyValue() {
      return new ArrayList<>();
    }
  }
}
