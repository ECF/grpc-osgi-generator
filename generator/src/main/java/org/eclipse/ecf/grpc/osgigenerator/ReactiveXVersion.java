// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: options.proto

package org.eclipse.ecf.grpc.osgigenerator;

/**
 * Protobuf enum {@code ReactiveXVersion}
 */
public enum ReactiveXVersion
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>V_2 = 0;</code>
   */
  V_2(0),
  /**
   * <code>V_3 = 1;</code>
   */
  V_3(1),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>V_2 = 0;</code>
   */
  public static final int V_2_VALUE = 0;
  /**
   * <code>V_3 = 1;</code>
   */
  public static final int V_3_VALUE = 1;


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
  public static ReactiveXVersion valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static ReactiveXVersion forNumber(int value) {
    switch (value) {
      case 0: return V_2;
      case 1: return V_3;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<ReactiveXVersion>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      ReactiveXVersion> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<ReactiveXVersion>() {
          public ReactiveXVersion findValueByNumber(int number) {
            return ReactiveXVersion.forNumber(number);
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
    return org.eclipse.ecf.grpc.osgigenerator.OsgiServiceOptionsProto.getDescriptor().getEnumTypes().get(2);
  }

  private static final ReactiveXVersion[] VALUES = values();

  public static ReactiveXVersion valueOf(
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

  private ReactiveXVersion(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:ReactiveXVersion)
}

