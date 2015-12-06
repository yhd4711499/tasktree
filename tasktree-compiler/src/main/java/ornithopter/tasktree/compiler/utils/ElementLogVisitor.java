package ornithopter.tasktree.compiler.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import static javax.tools.Diagnostic.Kind.OTHER;

/**
 * @author Ornithopter on 2015/11/22.
 */
class ElementLogVisitor implements ElementVisitor<Void, Void> {
    private final ProcessingEnvironment processingEnv;

    public ElementLogVisitor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public Void visit(Element e, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visit:" + e);
        return null;
    }

    @Override
    public Void visit(Element e) {
        processingEnv.getMessager().printMessage(OTHER, "visit:" + e);
        return null;
    }

    @Override
    public Void visitPackage(PackageElement e, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitPackage:" + e);
        return null;
    }

    @Override
    public Void visitType(TypeElement e, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitType:" + e);
        return null;
    }

    @Override
    public Void visitVariable(VariableElement e, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitVariable:" + e);
        return null;
    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitExecutable:" + e);
        return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameterElement e, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitTypeParameter:" + e);
        return null;
    }

    @Override
    public Void visitUnknown(Element e, Void aVoid) {
        processingEnv.getMessager().printMessage(OTHER, "visitUnknown:" + e);
        return null;
    }
}
