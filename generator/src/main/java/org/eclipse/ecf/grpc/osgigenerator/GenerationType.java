// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: options.proto

package org.eclipse.ecf.grpc.osgigenerator;

/**
 * Protobuf enum {@code GenerationType}
 */
public enum GenerationType
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>REACTIVEX = 0;</code>
   */
  REACTIVEX(0),
  /**
   * <code>GRPC_UNARY = 1;</code>
   */
  GRPC_UNARY(1),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>REACTIVEX = 0;</code>
   */
  public static final int REACTIVEX_VALUE = 0;
  /**
   * <code>GRPC_UNARY = 1;</code>
   */
  public static final int GRPC_UNARY_VALUE = 1;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static GenerationType valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static GenerationType forNumber(int value) {
    switch (value) {
      case 0: return REACTIVEX;
      case 1: return GRPC_UNARY;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<GenerationType>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      GenerationType> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<GenerationType>() {
          public GenerationType findValueByNumber(int number) {
            return GenerationType.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return org.eclipse.ecf.grpc.osgigenerator.OsgiServiceOptionsProto.getDescriptor().getEnumTypes().get(0);
  }

  private static final GenerationType[] VALUES = values();

  public static GenerationType valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private GenerationType(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:GenerationType)
}

