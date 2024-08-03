package com.github.sudu.persistentidecaches.ccsearch

import com.github.sudu.persistentidecaches.javaparaser.JavaLexer
import com.github.sudu.persistentidecaches.javaparaser.JavaParser
import com.github.sudu.persistentidecaches.javaparaser.JavaParser.*
import com.github.sudu.persistentidecaches.javaparaser.JavaParserBaseListener
import com.github.sudu.persistentidecaches.symbols.Symbols
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker

class JavaSymbolListener : JavaParserBaseListener() {
    var symbols: Symbols = Symbols(ArrayList(), ArrayList(), ArrayList())

    override fun enterClassDeclaration(ctx: ClassDeclarationContext) {
        symbols.classOrInterfaceSymbols.add(ctx.identifier().text)
    }

    override fun enterEnumDeclaration(ctx: EnumDeclarationContext) {
        symbols.classOrInterfaceSymbols.add(ctx.identifier().text)
    }

    override fun enterInterfaceDeclaration(ctx: InterfaceDeclarationContext) {
        symbols.classOrInterfaceSymbols.add(ctx.identifier().text)
    }

    override fun enterFieldDeclaration(ctx: FieldDeclarationContext) {
        ctx.variableDeclarators().variableDeclarator().stream()
            .map { it.variableDeclaratorId() }
            .map { it.identifier() }
            .map { it.text }
            .forEach { e: String? -> symbols.fieldSymbols.add(e) }
    }

    override fun enterConstDeclaration(ctx: ConstDeclarationContext) {
        symbols.fieldSymbols.addAll(
            ctx.constantDeclarator().stream()
                .map { it.identifier() }
                .map { it.text }
                .toList()
        )
    }

    override fun enterInterfaceMethodDeclaration(ctx: InterfaceMethodDeclarationContext) {
        symbols.methodSymbols.add(ctx.interfaceCommonBodyDeclaration().identifier().text)
    }

    override fun enterRecordDeclaration(ctx: RecordDeclarationContext) {
        symbols.classOrInterfaceSymbols.add(ctx.identifier().text)
    }

    override fun enterMethodDeclaration(ctx: MethodDeclarationContext) {
        symbols.methodSymbols.add(ctx.identifier().text)
    }

    // we need it?
    override fun enterLocalVariableDeclaration(ctx: LocalVariableDeclarationContext) {
        super.enterLocalVariableDeclaration(ctx)
    }

    override fun enterLocalTypeDeclaration(ctx: LocalTypeDeclarationContext) {
        super.enterLocalTypeDeclaration(ctx)
    }

    companion object {
        @JvmStatic
        fun getSymbolsFromString(javaFile: String?): Symbols {
            val lexer = JavaLexer(CharStreams.fromString(javaFile))
            val tokens = CommonTokenStream(lexer)
            val parser = JavaParser(tokens)
            val tree: ParseTree = parser.compilationUnit()
            val walker = ParseTreeWalker()
            val listener = JavaSymbolListener()
            walker.walk(listener, tree)
            return listener.symbols
        }
    }
}
