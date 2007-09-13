/** A class representing the type of an ArrayToken.

 Copyright (c) 1997-2006 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 */
package ptolemy.data.type;

import ptolemy.data.ArrayToken;
import ptolemy.data.Token;
import ptolemy.graph.InequalityTerm;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;

//////////////////////////////////////////////////////////////////////////
//// ArrayType

/**

 A class representing the type of an ArrayToken.

 @author Steve Neuendorffer, Yuhong Xiong
 @version $Id: ArrayType.java,v 1.95 2006/10/24 23:50:42 cxh Exp $
 @since Ptolemy II 4.0
 @Pt.ProposedRating Red (cxh)
 @Pt.AcceptedRating Red (cxh)
 */
public class ArrayType extends StructuredType {
    /** Construct a new ArrayType with the specified type for the array
     *  elements. To leave the element type undeclared, use BaseType.UNKNOWN.
     *  @param elementType The type of the array elements.
     *  @exception IllegalArgumentException If the argument is null.
     */
    public ArrayType(Type elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException("Cannot create ArrayType "
                    + " with null elementType");
        }

        try {
            _declaredElementType = (Type) elementType.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new InternalErrorException("The specified type "
                    + elementType + " cannot be cloned.");
        }

        _elementType = _declaredElementType;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return a type constraint that can be used to contrain
     *  another typeable object to have a type related to an
     *  array whose element type is the type of the specified
     *  typeable.  A typical usage of this is as follows:
     *  <pre>
     *      output.setTypeAtLeast(ArrayType.arrayOf(input));
     *  </pre>
     *  where input and output are ports (this is the type
     *  constraint of SequenceToArray, for example).
     *  @param typeable A typeable.
     *  @return An InequalityTerm that can be passed to methods
     *   like setTypeAtLeast() of the Typeable interface.
     *  @throws IllegalActionException If the specified typeable
     *   cannot be set to an array type.
     */
    public static InequalityTerm arrayOf(Typeable typeable)
            throws IllegalActionException {
        return new TypeableArrayTypeTerm(typeable);
    }

    /** Return a deep copy of this ArrayType if it is a variable, or
     *  itself if it is a constant.
     *  @return An ArrayType.
     */
    public Object clone() {
        if (isConstant()) {
            return this;
        } else {
            ArrayType newObj = new ArrayType(_declaredElementType);

            try {
                newObj.updateType(this);
            } catch (IllegalActionException ex) {
                throw new InternalErrorException("ArrayType.clone: "
                        + "Cannot update new instance. " + ex.getMessage());
            }

            return newObj;
        }
    }

    /** Convert the argument token into an ArrayToken having this
     *  type, if lossless conversion can be done.  If the argument
     *  is not an ArrayToken, then the result is an array token with
     *  one entry, the argument.
     *  @param token A token.
     *  @return An ArrayToken.
     *  @exception IllegalActionException If lossless conversion
     *   cannot be done.
     */
    public Token convert(Token token) throws IllegalActionException {
        Type myElementType = getElementType();
        // Cannot convert to unknown element type.
        if (myElementType.equals(BaseType.UNKNOWN)) {
            if (token instanceof ArrayToken) {
                // Following the logic implemented in BaseType for UNKNOWN,
                // since every array token is a substitution instance for
                // {unknown}, just return the token.
                return token;
            }
            // If it's not an ArrayToken, then something is wrong.
            throw new IllegalActionException("Cannot convert " + token
                    + " to type {unknown}");
        }
        if (!(token instanceof ArrayToken)) {
            // NOTE: Added 7/17/06 by EAL to support type -> {type} conversion.
            Token[] contents = new Token[1];
            contents[0] = token;
            return new ArrayToken(myElementType, contents);
        }

        ArrayToken argumentArrayToken = (ArrayToken) token;

        if (myElementType.equals(argumentArrayToken.getElementType())) {
            return token;
        }

        Token[] argumentArray = argumentArrayToken.arrayValue();
        Token[] resultArray = new Token[argumentArray.length];

        try {
            for (int i = 0; i < argumentArray.length; i++) {
                resultArray[i] = myElementType.convert(argumentArray[i]);
            }
        } catch (IllegalActionException ex) {
            throw new IllegalActionException(null, ex, Token
                    .notSupportedConversionMessage(token, "int"));
        }

        if (resultArray.length < 1) {
            // Support your local zero length array.
            // actor/lib/test/auto/NilTokenTypeTest.xml requires this.
            Type argumentArrayElementType = argumentArrayToken.getElementType();
            try {
                return new ArrayToken(argumentArrayElementType);
            } catch (Exception ex) {
                throw new IllegalActionException(null, ex,
                        "Failed to construct an array of type "
                                + argumentArrayElementType);
            }
        }
        return new ArrayToken(myElementType, resultArray);
    }

    /** Return the depth of an array type. The depth of an
     *  array type is the number of times it 
     *  contains other structured types. For example, an array 
     *  of arrays has depth 2, and an array of arrays of records 
     *  has depth 3.
     *  @return the depth of a structured type.
     */
    public int depth() {
        int depth = 1;
        if (_elementType instanceof StructuredType) {
            depth += ((StructuredType) _elementType).depth();
        }
        return depth;
    }

    /** Determine if the argument represents the same ArrayType as this
     *  object.
     *  @param object Another object.
     *  @return True if the argument represents the same ArrayType as
     *   this object; false otherwise.
     */
    public boolean equals(Object object) {
        if (!(object instanceof ArrayType)) {
            return false;
        }

        return _elementType.equals(((ArrayType) object).getElementType());
    }

    /** Return a type constraint that can be used to contrain
     *  another typeable object to have a type related to the
     *  element type of the specified typeable.  As a side
     *  effect, the specified typeable is constrained to have an array
     *  type.  A typical usage of this is as follows:
     *  <pre>
     *      output.setTypeAtLeast(ArrayType.elementType(input));
     *  </pre>
     *  where input and output are ports. This forces the input
     *  port to have an array type and the output port to have
     *  a type at least that of the elements of input arrays.
     *  @param typeable An array-valued typeable.
     *  @return An InequalityTerm that can be passed to methods
     *   like setTypeAtLeast() of the Typeable interface.
     *  @throws IllegalActionException If the specified typeable
     *   cannot be set to an array type.
     */
    public static InequalityTerm elementType(Typeable typeable)
            throws IllegalActionException {
        typeable.setTypeAtLeast(ArrayType.ARRAY_BOTTOM);
        return new TypeableElementTypeTerm(typeable);
    }

    /** Return the type of the array elements.
     *  @return a Type.
     */
    public Type getElementType() {
        return _elementType;
    }

    /** Return the InequalityTerm representing the element type.
     *  @return An InequalityTerm.
     *  @see ptolemy.graph.InequalityTerm
     */
    public InequalityTerm getElementTypeTerm() {
        return _elemTypeTerm;
    }

    /** Return the class for tokens that this type represents.
     *  @return The class for tokens that this type represents.  
     */
    public Class getTokenClass() {
        return ArrayToken.class;
    }

    /** Return a hash code value for this object.
     */
    public int hashCode() {
        return _elementType.hashCode() + 2917;
    }

    /** Set the elements that have declared type BaseType.UNKNOWN (the leaf
     *  type variable) to the specified type.
     *  @param t the type to set the leaf type variable to.
     */
    public void initialize(Type t) {
        try {
            if (!isConstant()) {
                getElementTypeTerm().initialize(t);
            }
        } catch (IllegalActionException iae) {
            throw new InternalErrorException("ArrayType.initialize: Cannot "
                    + "initialize the element type to " + t + ". "
                    + iae.getMessage());
        }
    }

    /** Test if the argument type is compatible with this type.
     *  If this type is a constant, the argument is compatible if it is less
     *  than or equal to this type in the type lattice; If this type is a
     *  variable, the argument is compatible if it is a substitution
     *  instance of this type.
     *  @param type A Type.
     *  @return True if the argument is compatible with this type.
     *  @see ptolemy.data.type.ArrayType#convert
     */
    public boolean isCompatible(Type type) {
        ArrayType arrayType;

        if (type instanceof ArrayType) {
            arrayType = (ArrayType) type;
        } else {
            return false;
        }

        Type elementType = arrayType.getElementType();
        return _elementType.isCompatible(elementType);
    }

    /** Test if this ArrayType is a constant. An ArrayType is a constant if
     *  it does not contain BaseType.UNKNOWN in any level.
     *  @return True if this type is a constant.
     */
    public boolean isConstant() {
        return _declaredElementType.isConstant();
    }

    /** Determine if this type corresponds to an instantiable token
     *  class. An ArrayType is instantiable if its element type is
     *  instantiable.
     *  @return True if this type is instantiable.
     */
    public boolean isInstantiable() {
        return _elementType.isInstantiable();
    }

    /** Return true if the specified type is a substitution instance of this
     *  type.
     *  @param type A Type.
     *  @return True if the argument is a substitution instance of this type.
     *  @see Type#isSubstitutionInstance
     */
    public boolean isSubstitutionInstance(Type type) {
        if (!(type instanceof ArrayType)) {
            return false;
        }

        Type argElemType = ((ArrayType) type).getElementType();
        return _declaredElementType.isSubstitutionInstance(argElemType);
    }

    /** Set the type to the specified type, which is required to be
     *  an array type.
     *  @param type The new type.
     *  @exception IllegalActionException If the specified type is not
     *   an instance of ArrayType.
     */
    public void setType(Type type) throws IllegalActionException {
        if (!(type instanceof ArrayType)) {
            throw new IllegalActionException(
                    "Cannot change an array type to a non-array type.");
        }
        try {
            Type clone = (Type) ((ArrayType) type).getElementType().clone();
            _elementType = clone;
            _declaredElementType = clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalErrorException(e);
        }
    }

    /** Return the string representation of this type. The format is
     *  {<i>type</i>}, where <i>type</i> is the element type.
     *  @return A String.
     */
    public String toString() {
        return "{" + _elementType.toString() + "}";
    }

    /** Update this Type to the specified ArrayType.
     *  The specified type must be an ArrayType with the same structure as
     *  this type, and have depth less than the MAXDEPTHDOUND.
     *  This method will only update the component whose declared type is
     *  BaseType.UNKNOWN, and leave the constant part of this type intact.
     *  @param newType A StructuredType.
     *  @exception IllegalActionException If the specified type is not an
     *   ArrayType or it does not have the same structure as this one.
     */
    public void updateType(StructuredType newType)
            throws IllegalActionException {
        super.updateType(newType);
        if (this.isConstant()) {
            if (this.equals(newType)) {
                return;
            } else {
                throw new IllegalActionException("ArrayType.updateType: "
                        + "This type is a constant and the argument is not "
                        + "the same as this type. " + "This type: "
                        + this.toString() + " argument: " + newType.toString());
            }
        }

        // This type is a variable.
        if (!this.isSubstitutionInstance(newType)) {
            throw new IllegalActionException("ArrayType.updateType: "
                    + "The type " + this + " cannot be updated to " + newType
                    + ".");
        }

        Type newElemType = ((ArrayType) newType).getElementType();

        if (_declaredElementType.equals(BaseType.UNKNOWN)) {
            try {
                _elementType = (Type) newElemType.clone();
            } catch (CloneNotSupportedException cnse) {
                throw new InternalErrorException("ArrayType.updateType: "
                        + "The specified element type cannot be cloned: "
                        + _elementType);
            }
        } else {
            // _declaredElementType is a StructuredType. _elementType
            // must also be.
            ((StructuredType) _elementType)
                    .updateType((StructuredType) newElemType);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    /** A term to use when declaring the type of some parameter or port
     *  to be an array.  The way to use this is to declare:
     *  <pre>
     *     param.setTypeAtLeast(ArrayType.ARRAY_BOTTOM);
     *  </pre>
     *  for a parameter "param".
     */
    public static InequalityTerm ARRAY_BOTTOM = (new ArrayType(BaseType.UNKNOWN) {
        // This particular inequality term always has an acceptable type
        // because it has no visible array that will ever be evaluated.
        // It is essential that isValueAcceptable() return true, or the
        // idiom above will result in reported type errors.
        public InequalityTerm getElementTypeTerm() {
            return _replacementElementTerm;
        }

        private InequalityTerm _replacementElementTerm = new ElementTypeTerm() {
            public boolean isValueAcceptable() {
                return true;
            }
        };
    }).getElementTypeTerm();

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Compare this type with the specified type. The specified type
     *  must be an ArrayType, otherwise an exception will be thrown.
     *  This method returns one of ptolemy.graph.CPO.LOWER,
     *  ptolemy.graph.CPO.SAME, ptolemy.graph.CPO.HIGHER,
     *  ptolemy.graph.CPO.INCOMPARABLE, indicating this type is lower
     *  than, equal to, higher than, or incomparable with the
     *  specified type in the type hierarchy, respectively.
     *  @param type an ArrayType.
     *  @return An integer.
     *  @exception IllegalArgumentException If the specified type is
     *   not an ArrayType.
     */
    protected int _compare(StructuredType type) {
        if (!(type instanceof ArrayType)) {
            throw new IllegalArgumentException("ArrayType.compare: "
                    + "The argument " + type + " is not an ArrayType.");
        }

        return TypeLattice.compare(_elementType, ((ArrayType) type)
                .getElementType());
    }

    /** Return a static instance of ArrayType.
     *  @return an ArrayType.
     */
    protected StructuredType _getRepresentative() {
        return _representative;
    }

    /** Return the greatest lower bound of this type with the specified
     *  type. The specified type must be an ArrayType, otherwise an
     *  exception will be thrown.
     *  @param type an ArrayType.
     *  @return an ArrayType.
     *  @exception IllegalArgumentException If the specified type is
     *   not an ArrayType.
     */
    protected StructuredType _greatestLowerBound(StructuredType type) {
        if (!(type instanceof ArrayType)) {
            throw new IllegalArgumentException("ArrayType.greatestLowerBound: "
                    + "The argument " + type + " is not an ArrayType.");
        }

        Type elementGLB = (Type) TypeLattice.lattice().greatestLowerBound(
                _elementType, ((ArrayType) type).getElementType());
        return new ArrayType(elementGLB);
    }

    /** Return the least Upper bound of this type with the specified
     *  type. The specified type must be an ArrayType, otherwise an
     *  exception will be thrown.
     *  @param type an ArrayType.
     *  @return an ArrayType.
     *  @exception IllegalArgumentException If the specified type is
     *   not an ArrayType.
     */
    protected StructuredType _leastUpperBound(StructuredType type) {
        if (!(type instanceof ArrayType)) {
            throw new IllegalArgumentException("ArrayType.leastUpperBound: "
                    + "The argument " + type + " is not an ArrayType.");
        }

        Type elementLUB = (Type) TypeLattice.lattice().leastUpperBound(
                _elementType, ((ArrayType) type).getElementType());
        return new ArrayType(elementLUB);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
    // the type of array elements.
    private Type _declaredElementType;

    private Type _elementType;

    private ElementTypeTerm _elemTypeTerm = new ElementTypeTerm();

    private static ArrayType _representative = new ArrayType(BaseType.UNKNOWN);

    ///////////////////////////////////////////////////////////////////
    ////                         inner classes                     ////

    /** An InequalityTerm associated with an instance of ArrayType. */
    private class ElementTypeTerm implements InequalityTerm {
        ///////////////////////////////////////////////////////////////
        ////                   public inner methods                ////

        /** Return this ArrayType.
         *  @return an ArrayType.
         */
        public Object getAssociatedObject() {
            return ArrayType.this;
        }

        /** Return the element type.
         *  @return a Type.
         */
        public Object getValue() {
            return _elementType;
        }

        /** Return this ElementTypeTerm in an array if this term
         *  represents a type variable. Otherwise, return an array of
         *  size zero.
         *  @return An array of InequalityTerm.
         */
        public InequalityTerm[] getVariables() {
            if (isSettable()) {
                InequalityTerm[] variable = new InequalityTerm[1];
                variable[0] = this;
                return variable;
            }

            return (new InequalityTerm[0]);
        }

        /** Reset the variable part of the element type to the specified
         *  type.
         *  @parameter e A Type.
         *  @exception IllegalActionException If this type is a constant,
         *   or the argument is not a Type.
         */
        public void initialize(Object e) throws IllegalActionException {
            if (isConstant()) {
                throw new IllegalActionException(
                        "ArrayType$ElementTypeTerm.initialize: " + "This type "
                                + this + " is not settable.");
            }

            if (!(e instanceof Type)) {
                throw new IllegalActionException(
                        "ArrayType$ElementTypeTerm.initialize: "
                                + "The argument " + this + " is not a Type.");
            }

            if (_declaredElementType.equals(BaseType.UNKNOWN)) {
                _elementType = (Type) e;
            } else {
                // element type is a structured type.
                ((StructuredType) _elementType).initialize((Type) e);
            }
        }

        /** Test if the element type is a type variable.
         *  @return True if the element type is a type variable.
         */
        public boolean isSettable() {
            return !_declaredElementType.isConstant();
        }

        /** Check whether the current element type is acceptable.
         *  The element type is acceptable if it represents an
         *  instantiable object.
         *  @return True if the element type is acceptable.
         */
        public boolean isValueAcceptable() {
            return _elementType.isInstantiable();
        }

        /** Set the element type to the specified type.
         *  @param e a Type.
         *  @exception IllegalActionException If the specified type violates
         *   the declared type of the element.
         */
        public void setValue(Object e) throws IllegalActionException {
            if (!isSettable()) {
                throw new IllegalActionException(
                        "ArrayType$ElementTypeTerm.setValue: This type " + e
                                + " is not settable.");
            }

            if (!_declaredElementType.isSubstitutionInstance((Type) e)) {
                // The LUB of the _elementType and another type is General,
                // this is a type conflict.
                throw new IllegalActionException(
                        "ArrayType$ElementTypeTerm.setValue: "
                                + "Cannot update the element type of this array to "
                                + "the new type." + " Element type: "
                                + _declaredElementType.toString()
                                + ", New type: " + e.toString());
            }

            if (_declaredElementType.equals(BaseType.UNKNOWN)) {
                try {
                    _elementType = (Type) ((Type) e).clone();
                } catch (CloneNotSupportedException cnse) {
                    throw new InternalErrorException(
                            "ArrayType$ElementTypeTerm.setValue: "
                                    + "The specified type " + e
                                    + " cannot be cloned.");
                }
            } else {
                ((StructuredType) _elementType).updateType((StructuredType) e);
            }
        }

        /** Return a string representation of this term.
         *  @return A String.
         */
        public String toString() {
            return "(ArrayElementType(" + getAssociatedObject() + "), "
                    + getValue() + ")";
        }
    }

    /** An InequalityTerm representing an array types whose elements
     *  have the type of the specified typeable.  The purpose of this class
     *  is to defer to as late as possible actually accessing
     *  the type of the typeable, since it may change dynamically.
     *  This term is not variable and cannot be set.
     */
    private static class TypeableArrayTypeTerm implements InequalityTerm {

        /** Construct a term that will defer to the type of the
         *  specified typeable.
         *  @param typeable The object to defer requests to.
         */
        public TypeableArrayTypeTerm(Typeable typeable) {
            _typeable = typeable;
        }

        ///////////////////////////////////////////////////////////////
        ////                   public inner methods                ////

        /** Return an array type with element types given by the associated typeable.
         *  @return An ArrayType.
         */
        public Object getAssociatedObject() {
            return _getArrayType();
        }

        /** Return an array type with element types given by the associated typeable.
         *  @return An ArrayType.
         *  @throws IllegalActionException If the type of the associated typeable
         *   cannot be determined.
         */
        public Object getValue() throws IllegalActionException {
            return _getArrayTypeRaw();
        }

        /** Return an array of size zero.
         *  @return An array of InequalityTerm.
         */
        public InequalityTerm[] getVariables() {
            return (new InequalityTerm[0]);
        }

        /** Throw an exception. This term cannot be set.
         *  @parameter e A Type.
         *  @exception IllegalActionException If this type is a constant,
         *   or the argument is not a Type.
         */
        public void initialize(Object e) throws IllegalActionException {
            throw new IllegalActionException(
                    "ArrayType$TypeableArrayTypeTerm.initialize: "
                            + "This array type given with elements given by "
                            + _typeable + " is not settable.");
        }

        /** Return false.
         *  @return False.
         */
        public boolean isSettable() {
            return false;
        }

        /** Delegate to an array type with elements given by the
         *  type of the associated typeable.
         *  @return True if the element type is acceptable.
         */
        public boolean isValueAcceptable() {
            ArrayType type = _getArrayType();
            return type.getElementTypeTerm().isValueAcceptable();
        }

        /** Throw an exception.
         *  @param type a Type.
         *  @exception IllegalActionException Always
         */
        public void setValue(Object type) throws IllegalActionException {
            throw new IllegalActionException(
                    "ArrayType$TypeableArrayTypeTerm.setValue: "
                            + "The array type with element type given by "
                            + _typeable + " is not settable.");
        }

        /** Delegate to an array type with elements given by the
         *  type of the associated typeable.
         *  @return A String.
         */
        public String toString() {
            return _getArrayType().toString();
        }

        ///////////////////////////////////////////////////////////////
        ////                   private methods                     ////

        /** Get an array type with element type matching the type
         *  of the associated typeable.
         *  @return An array type for the associated typeable.
         */
        private ArrayType _getArrayType() {
            try {
                return _getArrayTypeRaw();
            } catch (IllegalActionException e) {
                throw new InternalErrorException(e);
            }
        }

        /** Get an array type with element type matching the type
         *  of the associated typeable.
         *  @return An array type for the associated typeable.
         *  @throws IllegalActionException If the type of the typeable
         *   cannot be determined.
         */
        private ArrayType _getArrayTypeRaw() throws IllegalActionException {
            Type type = _typeable.getType();
            if (_arrayType == null || !_arrayType.getElementType().equals(type)) {
                _arrayType = new ArrayType(type);
            }
            return _arrayType;
        }

        ///////////////////////////////////////////////////////////////
        ////                   private members                     ////

        /** The associated typeable. */
        private Typeable _typeable;

        /** The array type with element types matching the typeable. */
        private ArrayType _arrayType;
    }

    /** An InequalityTerm representing the element types
     *  of an instance of Typeable.  The purpose of this class
     *  is to defer to as late as possible actually accessing
     *  the type of the typeable, since it may change dynamically.
     */
    private static class TypeableElementTypeTerm implements InequalityTerm {

        /** Construct a term that will defer to the type of the
         *  specified typeable.
         *  @param typeable The object to defer requests to.
         */
        public TypeableElementTypeTerm(Typeable typeable) {
            _typeable = typeable;
        }

        ///////////////////////////////////////////////////////////////
        ////                   public inner methods                ////

        /** Delegate to the element type term of the associated typeable.
         *  @return an ArrayType.
         */
        public Object getAssociatedObject() {
            return _getElementTypeTerm().getAssociatedObject();
        }

        /** Delegate to the element type term of the associated typeable.
         *  @return a Type.
         *  @throws IllegalActionException If the delegate throws it.
         */
        public Object getValue() throws IllegalActionException {
            return _getElementTypeTerm().getValue();
        }

        /** Delegate to the element type term of the associated typeable.
         *  @return An array of InequalityTerm.
         */
        public InequalityTerm[] getVariables() {
            return _getElementTypeTerm().getVariables();
        }

        /** Delegate to the element type term of the associated typeable.
         *  @return an ArrayType.
         *  @param type A Type.
         *  @throws IllegalActionException If the delegate throws it.
         */
        public void initialize(Object type) throws IllegalActionException {
            _getElementTypeTerm().initialize(type);
        }

        /** Delegate to the element type term of the associated typeable.
         *  @return True if the element type is a type variable.
         */
        public boolean isSettable() {
            return _getElementTypeTerm().isSettable();
        }

        /** Delegate to the element type term of the associated typeable.
         *  @return True if the element type is acceptable.
         */
        public boolean isValueAcceptable() {
            return _getElementTypeTerm().isValueAcceptable();
        }

        /** Delegate to the element type term of the associated typeable.
         *  @param type a Type.
         *  @exception IllegalActionException If the specified type violates
         *   the declared type of the element.
         */
        public void setValue(Object type) throws IllegalActionException {
            _getElementTypeTerm().setValue(type);
        }

        /** Delegate to the element type term of the associated typeable.
         *  @return A String.
         */
        public String toString() {
            return _getElementTypeTerm().toString();
        }

        ///////////////////////////////////////////////////////////////
        ////                   private methods                     ////

        /** Get an inequality term for elements of the
         *  associated typeable. If the associated typeable does
         *  not already have an array type, then assign it the type
         *  {unknown} and return its element type term.
         *  @return An array type for the associated typeable.
         */
        private InequalityTerm _getElementTypeTerm() {
            try {
                Type type = _typeable.getType();
                if (!(type instanceof ArrayType)) {
                    // Have to also declare the type equal to {unknown} to get
                    // a type term.  This will be changed to a more specific type
                    // during type resolution.
                    _typeable.setTypeEquals(new ArrayType(BaseType.UNKNOWN));
                    // Have to get the type again because it gets cloned.
                    type = _typeable.getType();
                }
                return ((ArrayType) type).getElementTypeTerm();
            } catch (IllegalActionException e) {
                throw new InternalErrorException(e);
            }
        }

        ///////////////////////////////////////////////////////////////
        ////                   private members                     ////

        /** The associated typeable. */
        private Typeable _typeable;
    }
}
