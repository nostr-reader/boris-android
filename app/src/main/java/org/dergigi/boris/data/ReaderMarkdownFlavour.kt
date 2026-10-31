package org.dergigi.boris.data

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.flavours.gfm.lexer._GFMLexer
import org.intellij.markdown.lexer.GeneratedLexer
import org.intellij.markdown.lexer.MarkdownLexer

/** GFM without dollar-delimited math, which the reader does not render. */
class ReaderMarkdownFlavour : GFMFlavourDescriptor() {
    override fun createInlinesLexer(): MarkdownLexer {
        val lexer = _GFMLexer()
        return MarkdownLexer(object : GeneratedLexer by lexer {
            // Preserve currency signs and prose between them, including lone dollars.
            // Keeping the original offsets also preserves selection/highlight mappings.
            override fun advance() = lexer.advance()?.let {
                if (it == GFMTokenTypes.DOLLAR) MarkdownTokenTypes.TEXT else it
            }
        })
    }
}
