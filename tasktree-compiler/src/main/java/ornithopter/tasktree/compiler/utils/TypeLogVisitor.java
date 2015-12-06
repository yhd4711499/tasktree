package ornithopter.tasktree.compiler.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

import static javax.tools.Diagnostic.Kind.OTHER;

/**
 * @author Ornithopter on 2015/11/28.
 */
class TypeLogVisitor implements TypeVisitor<Void, Void> {
    private final ProcessingEnvironment processingEnv;

    public TypeLogVisitor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public Void visit(TypeMirror t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visit :" + t);
        return null;
    }

    @Override
    public Void visit(TypeMirror t) {
        processingEnv.getMessager().printMessage(OTHER, "visit :" + t);
        return null;
    }

    @Override
    public Void visitPrimitive(PrimitiveType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitPrimitive: " + t);
        return null;
    }

    @Override
    public Void visitNull(NullType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitNull: " + t);
        return null;
    }

    @Override
    public Void visitArray(ArrayType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitArray: " + t);
        return null;
    }

    @Override
    public Void visitDeclared(DeclaredType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitDeclared: " + t);
        return null;
    }

    @Override
    public Void visitError(ErrorType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitError: " + t);
        return null;
    }

    @Override
    public Void visitTypeVariable(TypeVariable t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitTypeVariable: " + t);
        return null;
    }

    @Override
    public Void visitWildcard(WildcardType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitWildcard: " + t);
        return null;
    }

    @Override
    public Void visitExecutable(ExecutableType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitExecutable: " + t);
        return null;
    }

    @Override
    public Void visitNoType(NoType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visit NoType:: " + t);
        return null;
    }

    @Override
    public Void visitUnknown(TypeMirror t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitUnknown: " + t);
        return null;
    }

    @Override
    public Void visitUnion(UnionType t, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitUnion: " + t);
        return null;
    }

    @Override
    public Void visitIntersection(IntersectionType t, Void aVoid) {
        return null;
    }
}
