package com.github.SuduIDE.persistentidecaches.ccsearch;

import com.github.SuduIDE.persistentidecaches.javaparaser.JavaLexer;
import com.github.SuduIDE.persistentidecaches.javaparaser.JavaParser;
import com.github.SuduIDE.persistentidecaches.javaparaser.JavaParserBaseListener;
import com.github.SuduIDE.persistentidecaches.symbols.Symbols;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;

public class JavaSymbolListener extends JavaParserBaseListener {

    Symbols symbols = new Symbols(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    public static Symbols getSymbolsFromString(final String javaFile) {
        final JavaLexer lexer = new JavaLexer(CharStreams.fromString(javaFile));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final JavaParser parser = new JavaParser(tokens);
        final ParseTree tree = parser.compilationUnit();
        final ParseTreeWalker walker = new ParseTreeWalker();
        final JavaSymbolListener listener = new JavaSymbolListener();
        walker.walk(listener, tree);
        return listener.symbols;
    }

    @Override
    public void enterClassDeclaration(final JavaParser.ClassDeclarationContext ctx) {
        symbols.classOrInterfaceSymbols().add(ctx.identifier().getText());
    }

    @Override
    public void enterEnumDeclaration(final JavaParser.EnumDeclarationContext ctx) {
        symbols.classOrInterfaceSymbols().add(ctx.identifier().getText());
    }

    @Override
    public void enterInterfaceDeclaration(final JavaParser.InterfaceDeclarationContext ctx) {
        symbols.classOrInterfaceSymbols().add(ctx.identifier().getText());
    }

    @Override
    public void enterFieldDeclaration(final JavaParser.FieldDeclarationContext ctx) {
        ctx.variableDeclarators().variableDeclarator().stream()
                .map(JavaParser.VariableDeclaratorContext::variableDeclaratorId)
                .map(JavaParser.VariableDeclaratorIdContext::identifier)
                .map(RuleContext::getText)
                .forEach(symbols.fieldSymbols()::add);
    }

    @Override
    public void enterConstDeclaration(final JavaParser.ConstDeclarationContext ctx) {
        symbols.fieldSymbols().addAll(
                ctx.constantDeclarator().stream()
                        .map(JavaParser.ConstantDeclaratorContext::identifier)
                        .map(RuleContext::getText)
                        .toList()
        );
    }

    @Override
    public void enterInterfaceMethodDeclaration(final JavaParser.InterfaceMethodDeclarationContext ctx) {
        symbols.methodSymbols().add(ctx.interfaceCommonBodyDeclaration().identifier().getText());
    }

    @Override
    public void enterRecordDeclaration(final JavaParser.RecordDeclarationContext ctx) {
        symbols.classOrInterfaceSymbols().add(ctx.identifier().getText());
    }

    @Override
    public void enterMethodDeclaration(final JavaParser.MethodDeclarationContext ctx) {
        symbols.methodSymbols().add(ctx.identifier().getText());
    }

    // we need it?
    @Override
    public void enterLocalVariableDeclaration(final JavaParser.LocalVariableDeclarationContext ctx) {
        super.enterLocalVariableDeclaration(ctx);
    }

    @Override
    public void enterLocalTypeDeclaration(final JavaParser.LocalTypeDeclarationContext ctx) {
        super.enterLocalTypeDeclaration(ctx);
    }
}
