<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:variable name="fo:layout-master-set">
        <fo:layout-master-set>
            <fo:simple-page-master master-name="default-page" page-height="11.69in" page-width="8.27in" margin-left="0.6in" margin-right="0.6in">
                <fo:region-body margin-top="0.79in" margin-bottom="0.79in" />
                <fo:region-after extent="0.79in" />
            </fo:simple-page-master>
        </fo:layout-master-set>
    </xsl:variable>
    <xsl:output version="1.0" encoding="UTF-8" indent="no" omit-xml-declaration="no" media-type="text/html" />
    <xsl:template match="/">
        <fo:root>
            <xsl:copy-of select="$fo:layout-master-set" />
            <fo:page-sequence master-reference="default-page" initial-page-number="1" format="1">
                <fo:static-content flow-name="xsl-region-after" display-align="after">
                    <fo:block>
                        <fo:table width="100%" space-before.optimum="1pt" space-after.optimum="2pt">
                            <fo:table-column />
                            <fo:table-column column-width="150pt" />
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell padding-bottom="0pt" padding-left="0pt" padding-right="0pt" padding-top="0pt" border-style="solid" border-width="1pt" border-color="white" height="30pt" number-columns-spanned="2" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" text-align="start">
                                        <fo:block />
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell padding-bottom="0pt" padding-left="0pt" padding-right="0pt" padding-top="0pt" border-style="solid" border-width="1pt" border-color="white" number-columns-spanned="2" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" text-align="start">
                                        <fo:block>
                                            <fo:block color="black" space-before.optimum="-8pt">
                                                <fo:leader leader-length="100%" leader-pattern="rule" rule-thickness="1pt" />
                                            </fo:block>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell font-size="inherited-property-value(&apos;font-size&apos;) - 2pt" padding-bottom="0pt" padding-left="0pt" padding-right="0pt" padding-top="0pt" border-style="solid" border-width="1pt" border-color="white" text-align="left" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center">
                                        <fo:block />
                                    </fo:table-cell>
                                    <fo:table-cell font-size="inherited-property-value(&apos;font-size&apos;) - 2pt" padding-bottom="0pt" padding-left="0pt" padding-right="0pt" padding-top="0pt" border-style="solid" border-width="1pt" border-color="white" text-align="right" width="150pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center">
                                        <fo:block>
                                            <fo:inline font-weight="bold">Page: </fo:inline>
                                            <fo:page-number font-weight="bold" />
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body">
                    <fo:block>
                        <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                            <fo:block>
                                <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                                    <fo:block>
                                        <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                                            <fo:block>
                                                <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                                                    <fo:block>
                                                        <xsl:for-each select="checklist">
                                                            <xsl:for-each select="@name">
                                                                <fo:inline color="#183A53" font-size="20pt" font-style="normal" font-weight="bold">
                                                                    <xsl:value-of select="." />
                                                                </fo:inline>
                                                            </xsl:for-each>
                                                        </xsl:for-each>
                                                        <fo:inline color="#183A53" font-size="20pt" font-style="normal" font-weight="bold"> Checklist</fo:inline>
                                                    </fo:block>
                                                </fo:block>
                                                <xsl:for-each select="checklist">
                                                    <xsl:for-each select="description">
                                                        <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                                                            <fo:block>
                                                                <fo:inline font-weight="bold">Description:</fo:inline>
                                                                <fo:block>
                                                                    <fo:leader leader-pattern="space" />
                                                                </fo:block>
                                                                <fo:inline color="#808080">
                                                                    <xsl:value-of select="." />
                                                                </fo:inline>
                                                            </fo:block>
                                                        </fo:block>
                                                    </xsl:for-each>
                                                </xsl:for-each>
                                                <fo:block>
                                                    <xsl:text>&#xA;</xsl:text>
                                                </fo:block>
                                            </fo:block>
                                        </fo:block>
                                    </fo:block>
                                </fo:block>
                            </fo:block>
                        </fo:block>
                        <fo:block color="black" space-before.optimum="-8pt">
                            <fo:leader leader-length="100%" leader-pattern="rule" rule-thickness="1pt" />
                        </fo:block>
                        <xsl:for-each select="checklist">
                            <fo:block>
                                <xsl:text>&#xA;</xsl:text>
                            </fo:block>
                            <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                                <fo:block>
                                    <xsl:for-each select="item">
                                        <xsl:for-each select="@checked">
                                            <fo:inline padding-before="-3pt" padding-after="-2pt" text-decoration="underline" color="black">
                                                <fo:inline>
                                                    <xsl:choose>
                                                        <xsl:when test=".='true'">
                                                            <fo:inline white-space-collapse="false" font-family="ZapfDingbats" font-size="10pt" padding-start="1pt" padding-end="1pt">&#x2714;</fo:inline>
                                                        </xsl:when>
                                                        <xsl:when test=".='1'">
                                                            <fo:inline white-space-collapse="false" font-family="ZapfDingbats" font-size="10pt" padding-start="1pt" padding-end="1pt">&#x2714;</fo:inline>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <fo:inline text-decoration="underline" color="black">
                                                                <fo:leader leader-length="8pt" leader-pattern="rule" />
                                                            </fo:inline>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </fo:inline>
                                            </fo:inline>
                                        </xsl:for-each>
                                        <fo:inline> - </fo:inline>
                                        <xsl:for-each select="@name">
                                            <fo:inline>
                                                <xsl:value-of select="." />
                                            </fo:inline>
                                        </xsl:for-each>
                                        <fo:inline> (</fo:inline>
                                        <xsl:for-each select="@date-checked">
                                            <fo:inline>
                                                <xsl:value-of select="." />
                                            </fo:inline>
                                        </xsl:for-each>
                                        <fo:inline>)</fo:inline>
                                        <fo:block>
                                            <fo:leader leader-pattern="space" />
                                        </fo:block>
                                        <fo:inline>&#160;&#160;&#160;&#160;&#160;&#160;&#160; </fo:inline>
                                        <xsl:for-each select="note">
                                            <fo:inline font-weight="bold">Note:</fo:inline>
                                            <fo:inline>&#160;</fo:inline>
                                            <fo:inline>
                                                <xsl:apply-templates />
                                            </fo:inline>
                                        </xsl:for-each>
                                        <fo:block>
                                            <fo:leader leader-pattern="space" />
                                        </fo:block>
                                        <fo:block>
                                            <fo:leader leader-pattern="space" />
                                        </fo:block>
                                    </xsl:for-each>
                                </fo:block>
                            </fo:block>
                            <xsl:for-each select="group">
                                <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                                    <fo:block>
                                        <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
                                            <fo:block>
                                                <xsl:for-each select="@name">
                                                    <fo:inline color="#004080">
                                                        <xsl:value-of select="." />
                                                    </fo:inline>
                                                </xsl:for-each>
                                            </fo:block>
                                        </fo:block>
                                    </fo:block>
                                </fo:block>
                                <xsl:for-each select="item">
                                    <xsl:for-each select="@checked">
                                        <fo:inline padding-before="-3pt" padding-after="-2pt" text-decoration="underline" color="black">
                                            <fo:inline>
                                                <xsl:choose>
                                                    <xsl:when test=".='true'">
                                                        <fo:inline white-space-collapse="false" font-family="ZapfDingbats" font-size="10pt" padding-start="1pt" padding-end="1pt">&#x2714;</fo:inline>
                                                    </xsl:when>
                                                    <xsl:when test=".='1'">
                                                        <fo:inline white-space-collapse="false" font-family="ZapfDingbats" font-size="10pt" padding-start="1pt" padding-end="1pt">&#x2714;</fo:inline>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <fo:inline text-decoration="underline" color="black">
                                                            <fo:leader leader-length="8pt" leader-pattern="rule" />
                                                        </fo:inline>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </fo:inline>
                                        </fo:inline>
                                    </xsl:for-each>
                                    <fo:inline> - </fo:inline>
                                    <xsl:for-each select="@name">
                                        <fo:inline>
                                            <xsl:value-of select="." />
                                        </fo:inline>
                                    </xsl:for-each>
                                    <fo:inline> (</fo:inline>
                                    <xsl:for-each select="@date-checked">
                                        <fo:inline>
                                            <xsl:value-of select="." />
                                        </fo:inline>
                                    </xsl:for-each>
                                    <fo:inline>)</fo:inline>
                                    <fo:block>
                                        <fo:leader leader-pattern="space" />
                                    </fo:block>
                                    <fo:inline>&#160;&#160;&#160;&#160;&#160;&#160;&#160; </fo:inline>
                                    <xsl:for-each select="note">
                                        <fo:inline font-weight="bold">Note:</fo:inline>
                                        <fo:inline>&#160;</fo:inline>
                                        <fo:inline>
                                            <xsl:apply-templates />
                                        </fo:inline>
                                    </xsl:for-each>
                                    <fo:block>
                                        <fo:leader leader-pattern="space" />
                                    </fo:block>
                                </xsl:for-each>
                                <fo:block>
                                    <fo:leader leader-pattern="space" />
                                </fo:block>
                            </xsl:for-each>
                            <fo:block>
                                <xsl:text>&#xA;</xsl:text>
                            </fo:block>
                        </xsl:for-each>
                    </fo:block>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
    <xsl:template match="checklist">
        <xsl:apply-templates />
    </xsl:template>
</xsl:stylesheet>
