package org.dergigi.boris.data

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Assert.*
import org.junit.Test

class ReaderMarkdownFlavourTest {
    private val flavour = ReaderMarkdownFlavour()

    @Test fun currencyDoesNotConsumeInterveningProse() {
        val markdown = "Fees were `19,435` sats, or $12.51 at current prices. " +
            "Given that we've sent over $2.3 million this quarter."
        val root = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        assertNoMath(root)
        val html = HtmlGenerator(markdown, root, flavour).generateHtml()
        assertTrue(html.contains("<code>19,435</code>"))
        assertTrue(html.contains("$12.51 at current prices. Given that we've sent over $2.3 million"))
    }

    @Test fun loneDollarsAndGfmFormattingRemainVisible() {
        val markdown = "## Budget $5\n\n**$1,000,000 worth of sats**, ~~old~~ and [details](https://example.com).\n\n" +
            "Literal $ signs and `$" + "HOME` stay visible."
        val root = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        assertNoMath(root)
        val html = HtmlGenerator(markdown, root, flavour).generateHtml()
        assertTrue(html.contains("Budget $5"))
        assertTrue(html.contains("<strong>$1,000,000 worth of sats</strong>"))
        assertTrue(html.contains("user-del"))
        assertTrue(html.contains("href=\"https://example.com\""))
        assertTrue(html.contains("Literal $ signs"))
        assertTrue(html.contains("<code>$" + "HOME</code>"))
    }

    private fun assertNoMath(node: ASTNode) {
        assertNotEquals(GFMElementTypes.INLINE_MATH, node.type)
        assertNotEquals(GFMElementTypes.BLOCK_MATH, node.type)
        assertNotEquals(GFMTokenTypes.DOLLAR, node.type)
        node.children.forEach(::assertNoMath)
    }
}
