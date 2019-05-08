package com.hzy.common.processors;

import com.hzy.common.annotations.AutoNewInterface;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author huangyang
 * @Description: AutoNewService 注解处理器
 * @date 2019/05/08 下午3:39
 */
public class AutoNewInterfaceProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    //用于错误信息
    private Messager messager;


    private static boolean ifAdded(ExecutableElement method) {
        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PUBLIC) && method.getAnnotation(Override.class) == null) {
            return true;
        }
        return false;
    }

    private static JavaFile newInterface(String packageName, String name, List<ExecutableElement> methods) throws ClassNotFoundException {

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(name).addModifiers(Modifier.PUBLIC);
        for (ExecutableElement method : methods) {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString());
            methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
            methodBuilder.returns(ClassName.get(method.getReturnType()));
            List<? extends VariableElement> parameters = method.getParameters();
            for (VariableElement p : parameters) {
                ParameterSpec parameterSpec = ParameterSpec.builder(ClassName.get(p.asType()), p.getSimpleName().toString(), null).build();
                methodBuilder.addParameter(parameterSpec);
            }
            builder.addMethod(methodBuilder.build());
        }
        return JavaFile.builder(packageName, builder.build()).build();

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement te : annotations) {

            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(te);

            if (elements == null || elements.isEmpty()) {
                return false;
            }

            for (Element element : elements) {

                ElementKind kind = element.getKind();
                if (kind != ElementKind.CLASS) {
                    error(element, "注解" + te.getSimpleName().toString() + "只能应用在类上");
                    return false;
                }

                AutoNewInterface annotation = element.getAnnotation(AutoNewInterface.class);

                String name = interfaceName((TypeElement) element, annotation.value());
                String packageName = packageName((TypeElement) element, annotation.packageName());

                List<ExecutableElement> methods = ElementFilter.methodsIn(element.getEnclosedElements());

                if (methods == null) {
                    return false;
                }
                List<ExecutableElement> toBeAddMethods = methods.stream().filter(f -> ifAdded(f)).collect(Collectors.toList());
                if (toBeAddMethods.isEmpty()) {
                    return false;
                }
                try {
                    JavaFile javaFile = newInterface(packageName, name, toBeAddMethods);
                    javaFile.writeTo(filer);
                } catch (Exception e) {
                    error(element, "自动创建接口[%s]异常,INFO: %s", packageName + "." + name, e.toString());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> sets = new HashSet<>();
        sets.add(AutoNewInterface.class.getCanonicalName());
        return sets;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    private String interfaceName(TypeElement element, String name) {
        if (name == null || "".equalsIgnoreCase(name)) {
            String simpleName = element.getSimpleName().toString();
            if (simpleName.endsWith("Impl")) {
                return simpleName.replace("Impl", "");
            } else {
                return "I" + simpleName;
            }
        }
        return name;
    }

    private String packageName(TypeElement element, String name) {
        if (name == null || "".equalsIgnoreCase(name)) {
            return elementUtils.getPackageOf(element).getQualifiedName().toString();
        }
        return name;
    }


    private void error(Element e, String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }
}
