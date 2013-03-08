<?xml version="1.0" encoding="UTF-8"?>
<!--
<head>
    <meta name="filename"    content="ant2html.xsl"/>
    <meta name="author(s)"   content="Jim Creasman (creasman@us.ibm.com), Advisory Programmer, IBM"/>
    <meta name="url"         content="http://download.boulder.ibm.com/ibmdl/pub/software/dw/library/x-antxslsource.zip"/>
    <meta name="created"     content="09 Sep 2003"/>
    <meta name="description" content="XSLT stylesheet, converts Ant build scripts into readable HTML"/>
    <meta name="version"     content="$Id$"/>
    <meta name="warning"     content="Does not work in IE7.  Please see: https://www.movesinstitute.org/bugzilla/show_bug.cgi?id=1693"/>
</head>
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="no" omit-xml-declaration="yes" encoding="UTF-8"/>

<xsl:template match="/">
  <html>
    <xsl:comment>XSLT stylesheet used to transform this file:  ant2html.xsl</xsl:comment>
    <xsl:apply-templates select="project"/>
  </html>
</xsl:template>

<xsl:template match="project">
  <head>
  <title>Ant Project Source: <xsl:value-of select="@name"/></title>
  </head>
  <body bgcolor="#ffffff" marginheight="2" marginwidth="2" topmargin="2" leftmargin="2">
    <table border="1" cellspacing="0" cellpadding="2">
      <tr>
        <td valign="TOP" width="20%">
          <h2><a name="toc">Table of Contents</a></h2>
          <b><big><a href="#project">Project Attributes</a></big></b><br/><br/>

          <b><big><a href="#properties">Properties</a></big></b><br/>
<!--
          <xsl:for-each select="./property/@name">
            <xsl:sort/>
            <xsl:variable name="propName" select="."/>
            <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;&amp;nbsp;</xsl:text>
            <xsl:element name="a">
              <xsl:attribute name="href">
                <xsl:value-of select="concat('#property-',$propName)"/>
              </xsl:attribute>
              <xsl:value-of select="$propName"/>
            </xsl:element>
            <br/>
          </xsl:for-each>
-->

          <br/>
          <a name="toc-targets"/>
          <b><big><a href="#targets">Targets</a></big></b><br/>
          <xsl:for-each select="./target">
            <xsl:sort select="@name"/>
            <xsl:variable name="tarName" select="@name"/>
            <xsl:text disable-output-escaping="no">- </xsl:text>
            <xsl:element name="a">
              <xsl:attribute name="href">
                <xsl:value-of select="concat('#target-',$tarName)"/>
              </xsl:attribute>
              <xsl:value-of select="$tarName"/>
            </xsl:element>
            <br/>
          </xsl:for-each>
        </td>

        <td valign="TOP" width="80%">
          <!-- Begin project data -->
          <table border="0" cellspacing="0" cellpadding="5">
            <tr>
              <td colspan="3">
                <a name="project"/>
                <b><big>Project Information</big></b>
              </td>
            </tr>
            <tr>
              <td width="5%"/>
              <td valign="BOTTOM" width="25%">
                <b>Name:</b>
              </td>
              <td valign="BOTTOM" width="70%">
                <xsl:value-of select="@name"/>
              </td>
            </tr>
            <tr>
              <td width="5%"/>
              <td valign="BOTTOM" width="25%">
                <b>Base directory:</b>
              </td>
              <td valign="BOTTOM" width="70%">
                <xsl:choose>
                  <xsl:when test="@basedir='.'">
                    <i>current-working-directory</i>
                  </xsl:when>

                  <xsl:otherwise>
                    <xsl:value-of select="@basedir"/>
                  </xsl:otherwise>
                </xsl:choose>
              </td>
            </tr>
            <tr>
              <td width="5%"/>
              <td valign="BOTTOM" width="25%">
                <b>Default target:</b>
              </td>
              <td valign="BOTTOM" width="70%">
                <xsl:call-template name="formatTargetList">
                  <xsl:with-param name="targets" select="@default"/>
                </xsl:call-template>
              </td>
            </tr>
          </table>

          <hr/>
          <!-- Begin project data -->
          <table border="0" cellspacing="0" cellpadding="5">
            <tr>
              <td colspan="3">
                <a name="properties"/>
                <b><big><a href="#toc">Project Properties</a></big></b>
              </td>
            </tr>
            <xsl:for-each select="./property/@name">
              <xsl:sort/>
              <tr>
                <td width="5%"/>
                <td valign="BOTTOM" width="25%">
                  <xsl:element name="a">
                    <xsl:attribute name="name">
                      <xsl:text>property-</xsl:text><xsl:value-of select="."/>
                    </xsl:attribute>
                  </xsl:element>
                  <b><xsl:value-of select="."/></b>
                </td>
                <td valign="BOTTOM" width="70%">
                  <xsl:choose>
                    <xsl:when test="count(../@location) > 0">
                      <xsl:value-of select="../@location"/>
                    </xsl:when>

                    <xsl:when test="count(../@value) > 0">
                      <xsl:value-of select="../@value"/>
                    </xsl:when>
                  </xsl:choose>
                </td>
              </tr>
            </xsl:for-each>
          </table>

          <hr/>
          <!-- Begin project data -->
          <table border="0" cellspacing="0" cellpadding="5">
            <tr>
              <td colspan="3">
                <a name="targets"/>
                <b><big><a href="#toc">Targets</a></big></b>
              </td>
            </tr>

            <xsl:for-each select="./target">
              <xsl:sort select="@name"/>
              <tr>
                <td width="5%"/>
                <td valign="BOTTOM" width="25%">
                  <xsl:element name="a">
                    <xsl:attribute name="name">
                      <xsl:text>target-</xsl:text><xsl:value-of select="@name"/>
                    </xsl:attribute>
                  </xsl:element>
                  <b>Target:</b>
                </td>
                <td valign="BOTTOM" width="70%">
                  <xsl:value-of select="@name"/>
                </td>
              </tr>

              <xsl:if test="count(./@description) > 0">
                <tr>
                  <td width="5%"/>
                  <td valign="TOP" width="25%">
                    <b>Description:</b>
                  </td>
                  <td valign="TOP" width="70%">
                    <xsl:value-of select="@description"/>
                  </td>
                </tr>
              </xsl:if>

              <xsl:if test="count(./@depends) > 0">
                <tr>
                  <td width="5%"/>
                  <td valign="TOP" width="25%">
                    <b>Dependencies:</b>
                  </td>
                  <td valign="TOP" width="70%">
                    <xsl:call-template name="formatTargetList">
                      <xsl:with-param name="targets" select="@depends"/>
                    </xsl:call-template>
                  </td>
                </tr>
              </xsl:if>

              <tr>
                <td width="5%"/>
                <td valign="TOP" width="25%">
                  <b>Tasks:</b>
                </td>
                <td valign="TOP" width="70%">
                  <xsl:choose>
                    <xsl:when test="count(child::node()) > 0">
                      <xsl:apply-templates select="child::node()"/>
                    </xsl:when>

                    <xsl:otherwise>
                      <xsl:text>None</xsl:text>
                    </xsl:otherwise>
                  </xsl:choose>
                </td>
              </tr>

              <tr>
                <td colspan="3">
                  <xsl:element name="a">
                    <xsl:attribute name="href">
                      <xsl:text>#toc-targets</xsl:text>
                    </xsl:attribute>
                    <xsl:text>Return to targets</xsl:text>
                  </xsl:element>
                </td>
              </tr>

              <tr>
                <td colspan="3">
                  <xsl:element name="a">
                    <xsl:attribute name="href">
                      <xsl:text>#toc</xsl:text>
                    </xsl:attribute>
                    <xsl:text>Return to table of contents</xsl:text>
                  </xsl:element>
                </td>
              </tr>

              <xsl:if test="position() &lt; last()">
                <tr><td colspan="3"><hr/></td></tr>
              </xsl:if>
            </xsl:for-each>
          </table>
        </td>
      </tr>
    </table>
  </body>
</xsl:template>

  <!--
    =========================================================================
      Purpose:  Copy each node and attribute, exactly as found, to the output
                tree.
    =========================================================================
  -->
  <xsl:template match="node()" name="writeTask">
    <xsl:param    name="indent"   select="string('')"/>
    <xsl:variable name="nodeName" select="name(.)"/>
<pre><xsl:value-of select="$indent"/>&lt;<xsl:value-of select="$nodeName"/>
      <xsl:if test="count(@*) > 0">
        <xsl:for-each select="@*">
          <xsl:if test="position() > 1"><xsl:value-of select="'&#10;'"/></xsl:if>
          <xsl:value-of select="concat('     ', $indent, name(),'=&quot;',.,'&quot;')"/>
          <xsl:if test="position() = last()">
            <xsl:choose>
              <xsl:when test="count(../*) > 0">&gt;</xsl:when>
              <xsl:otherwise>/&gt;</xsl:otherwise>
            </xsl:choose>
          </xsl:if>
        </xsl:for-each>
      </xsl:if>

      <xsl:for-each select="child::node()">
        <xsl:call-template name="writeTask">
          <xsl:with-param name="indent" select="concat($indent, '  ')"/>
        </xsl:call-template>
      </xsl:for-each>

<xsl:value-of select="$indent"/><xsl:if test="count(child::node()) > 0">&lt;/<xsl:value-of select="$nodeName"/>&gt;</xsl:if></pre>

  </xsl:template>

  <!--
    =========================================================================
      Purpose:  Ignore comments imbedded into the text.
    =========================================================================
  -->
  <xsl:template match="comment()"/>

  <!--
    =========================================================================
      Purpose:  Format a list of target names as references.
    =========================================================================
  -->
  <xsl:template name="formatTargetList">
    <xsl:param    name="targets" select="string('')"/>

    <xsl:variable name="list"    select="normalize-space($targets)"/>
    <xsl:variable name="first"   select="normalize-space(substring-before($targets,','))"/>
    <xsl:variable name="rest"    select="normalize-space(substring-after($targets,','))"/>

    <xsl:if test="not($list = '')">
      <xsl:choose>
        <xsl:when test="contains($list, ',')">
          <xsl:element name="a">
            <xsl:attribute name="href">
              <xsl:value-of select="concat('#target-', $first)"/>
            </xsl:attribute>
            <xsl:value-of select="$first"/>
          </xsl:element>

          <xsl:text>, </xsl:text>

          <xsl:call-template name="formatTargetList">
            <xsl:with-param name="targets" select="$rest"/>
          </xsl:call-template>
        </xsl:when>

        <xsl:otherwise>
          <xsl:element name="a">
            <xsl:attribute name="href">
              <xsl:value-of select="concat('#target-', $list)"/>
            </xsl:attribute>
            <xsl:value-of select="$list"/>
          </xsl:element>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
