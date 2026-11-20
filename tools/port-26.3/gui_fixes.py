"""
GUI pass: rewrites 1.12.2 immediate-mode GUI drawing (GlStateManager, GL11, Gui.drawRect, font.drawString, the render
engine's bindTexture, RenderItem) to BuildCraft's 26.3 GUI drawing context, buildcraft.lib.gui.GuiDraw, which keeps
1.12.2's "bound texture" and "current colour" state and draws into the screen's GuiGraphicsExtractor.

GL state that 26.3's GUI pipelines set themselves (blend, depth, lighting, alpha test, shade model, texture units) is
dropped. Run after rename.py on GUI code, then compile:

    python tools/port-26.3/gui_fixes.py <file-or-dir>...

Every rule is idempotent.
"""
import os
import re
import sys

ARG = r'((?:[^(),]|\([^()]*(?:\([^()]*\))*[^()]*\))+)'  # one argument, allowing nested calls two deep

# Receivers that are fonts (1.12.2 FontRenderer, now Font)
FONT = (r'(\b(?:this\.fontRenderer|fontRenderer|font|fr|mc\.font|Minecraft\.getInstance\(\)\.font|gui\.mc\.font|'
        r'getFontRenderer\(\)|gui\.getFontRenderer\(\)))')

RULES = [
    # Matrix
    (r'GlStateManager\.pushMatrix\(\);', 'GuiDraw.pushPose();'),
    (r'GlStateManager\.popMatrix\(\);', 'GuiDraw.popPose();'),
    (r'GL11\.glPushMatrix\(\);', 'GuiDraw.pushPose();'),
    (r'GL11\.glPopMatrix\(\);', 'GuiDraw.popPose();'),
    (r'GlStateManager\.translate\(' + ARG + ',' + ARG + ',' + ARG + r'\);', r'GuiDraw.translate(\1,\2);'),
    (r'GL11\.glTranslated?\(' + ARG + ',' + ARG + ',' + ARG + r'\);', r'GuiDraw.translate(\1,\2);'),
    (r'GlStateManager\.scale\(' + ARG + ',' + ARG + ',' + ARG + r'\);', r'GuiDraw.scale(\1,\2);'),
    (r'GL11\.glScaled?\(' + ARG + ',' + ARG + ',' + ARG + r'\);', r'GuiDraw.scale(\1,\2);'),
    # Colour
    (r'GlStateManager\.color\(' + ARG + ',' + ARG + ',' + ARG + ',' + ARG + r'\);',
     r'GuiDraw.colour((float) (\1), (float) (\2), (float) (\3), (float) (\4));'),
    (r'GlStateManager\.color\(' + ARG + ',' + ARG + ',' + ARG + r'\);',
     r'GuiDraw.colour((float) (\1), (float) (\2), (float) (\3), 1f);'),
    (r'GL11\.glColor[34][fd]\(' + ARG + ',' + ARG + ',' + ARG + ',' + ARG + r'\);',
     r'GuiDraw.colour((float) (\1), (float) (\2), (float) (\3), (float) (\4));'),
    (r'GL11\.glColor3[fd]\(' + ARG + ',' + ARG + ',' + ARG + r'\);',
     r'GuiDraw.colour((float) (\1), (float) (\2), (float) (\3), 1f);'),
    # Textures
    (r'(?:Minecraft\.getInstance\(\)|mc|gui\.mc|this\.mc)\.renderEngine\.bindTexture\(' + ARG + r'\);',
     r'GuiDraw.bind(\1);'),
    (r'(?:Minecraft\.getInstance\(\)|mc|gui\.mc|this\.mc)\.getTextureManager\(\)\.bindTexture\(' + ARG + r'\);',
     r'GuiDraw.bind(\1);'),
    # Rectangles
    (r'\bGui\.drawRect\(', 'GuiDraw.fill('),
    # Fonts
    (r'\.getStringWidth\(', '.width('),
    (r'\.FONT_HEIGHT\b', '.lineHeight'),
    (FONT + r'\.drawStringWithShadow\(' + ARG + ',' + ARG + ',' + ARG + ',' + ARG + r'\)',
     r'GuiDraw.text(\1, \2,\3,\4,\5, true)'),
    (FONT + r'\.drawString\(' + ARG + ',' + ARG + ',' + ARG + ',' + ARG + ',' + ARG + r'\)',
     r'GuiDraw.text(\1, \2,\3,\4,\5,\6)'),
    (FONT + r'\.drawString\(' + ARG + ',' + ARG + ',' + ARG + ',' + ARG + r'\)',
     r'GuiDraw.text(\1, \2,\3,\4,\5, false)'),
    # Screens: 1.12.2-style hooks GuiBC8 keeps (no checked exceptions any more)
    (r'(protected void (?:mouseClicked|keyTyped|mouseReleased|mouseClickMove)\([^)]*\)) throws IOException', r'\1'),
    (r'\bnet\.minecraft\.client\.gui\.GuiTextField\b', 'buildcraft.lib.gui.GuiTextFieldBC'),
    (r'\bnew GuiTextField\(', 'new GuiTextFieldBC('),
    (r'\bGuiTextField\b', 'GuiTextFieldBC'),
    (r'\bKeyboard\.KEY_ESCAPE\b', 'com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE'),
    (r'^import org\.lwjgl\.input\.Keyboard;\n', ''),
    (r'\bI18n\.format\(', 'I18n.get('),
    (r'\bTextureMap\.LOCATION_BLOCKS_TEXTURE\b', 'net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS'),
    (r'^import net\.minecraft\.client\.renderer\.texture\.TextureMap;\n', ''),
    # Items
    (r'[\w.()]*\.renderItemAndEffectIntoGUI\((?:[\w.()]+player,\s*)?' + ARG + ',' + ARG + ',' + ARG + r'\);',
     r'GuiDraw.item(\1,\2,\3);'),
    (r'[\w.()]*\.renderItemIntoGUI\(' + ARG + ',' + ARG + ',' + ARG + r'\);', r'GuiDraw.item(\1,\2,\3);'),
    (r'[\w.()]*\.renderItemOverlayIntoGUI\([^;]*\);', '// (item overlays are drawn by GuiDraw.item)'),
]

# Whole statements of GL state the 26.3 gui pipelines set themselves
DROP = re.compile(
    r'^\s*(?:GlStateManager\.(?:enable|disable)\w*\([^;]*\);|GlStateManager\.(?:blendFunc|tryBlendFuncSeparate|'
    r'shadeModel|setActiveTexture|alphaFunc|depthMask|colorMask|cullFace|glLineWidth|glTexParameteri|bindTexture|'
    r'clear|depthFunc)\([^;]*\);|RenderHelper\.\w+\(\);|GL11\.gl(?:Enable|Disable|BlendFunc|LineWidth|DepthMask|'
    r'ColorMask|AlphaFunc|ShadeModel)\([^;]*\);|OpenGlHelper\.\w+\([^;]*\);)\s*$')

DEAD_IMPORTS = re.compile(
    r'^import (?:org\.lwjgl\.opengl\.GL11|org\.lwjgl\.opengl\.GL12|net\.minecraft\.client\.renderer\.GlStateManager|'
    r'net\.minecraft\.client\.renderer\.RenderHelper|net\.minecraft\.client\.gui\.Gui|'
    r'net\.minecraft\.client\.renderer\.OpenGlHelper|net\.minecraft\.client\.renderer\.RenderItem|'
    r'net\.minecraft\.client\.renderer\.Tessellator|com\.mojang\.blaze3d\.vertex\.DefaultVertexFormat);\n', re.M)


def fix(text, fonts=True):
    out = []
    for line in text.split('\n'):
        if DROP.match(line):
            continue
        out.append(line)
    text = '\n'.join(out)
    for pattern, repl in RULES:
        if not fonts and (pattern.startswith(FONT) or 'getStringWidth' in pattern or 'FONT_HEIGHT' in pattern):
            continue
        text = re.sub(pattern, repl, text, flags=re.M)
    text = DEAD_IMPORTS.sub('', text)
    if 'GuiDraw.' in text and 'import buildcraft.lib.gui.GuiDraw;' not in text \
            and not re.search(r'^package buildcraft\.lib\.gui;', text, re.M):
        text = re.sub(r'^(package [\w.]+;\n)', r'\1\nimport buildcraft.lib.gui.GuiDraw;\n', text, count=1, flags=re.M)
    return text


def main(paths):
    # --no-font: leave drawString calls alone (code with its own font interface, like the guide's IFontRenderer)
    fonts = '--no-font' not in paths
    paths = [p for p in paths if p != '--no-font']
    changed = 0
    total = 0
    for root in paths:
        files = [root] if os.path.isfile(root) else [os.path.join(d, f) for d, _, fs in os.walk(root) for f in fs]
        for path in files:
            if not path.endswith('.java'):
                continue
            total += 1
            with open(path, encoding='utf-8') as f:
                text = f.read()
            new = fix(text, fonts)
            if new != text:
                with open(path, 'w', encoding='utf-8', newline='') as f:
                    f.write(new)
                changed += 1
    print('%d of %d files changed' % (changed, total))


if __name__ == '__main__':
    main(sys.argv[1:])
