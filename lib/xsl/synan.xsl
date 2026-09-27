<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="synan">
  <html>
    <style>
      table, tr, td {
      text-align: center;
      vertical-align: top;
      }
    </style>
    <body>
      <table>
	<tr>
	  <xsl:apply-templates/>
	</tr>
      </table>
    </body>
  </html>
</xsl:template>

<xsl:template match="node">
  <td>
    <table width="100%">
      <tr bgcolor="E9DD8F">
	<td colspan="1000">
	  <nobr>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    <font style="font-family:arial black">
	      <xsl:value-of select="@label"/>
	    </font>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  </nobr>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="node|token"/>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="token">
  <td>
    <table width="100%">
      <tr bgcolor="FFCF00">
	<td>
	  <nobr>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    <font style="font-family:arial black">
	      <xsl:value-of select="@kind"/>
	    </font>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  </nobr>
	  <xsl:if test="location">
	    <br/>
	    <nobr>
	      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      <xsl:apply-templates select="location"/>
	      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    </nobr>
	  </xsl:if>
	  <xsl:choose>
	    <xsl:when test="@lexeme!=''">
	      <br/>
	      <nobr>
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		<font style="font-family:courier new">
		  <xsl:value-of select="@lexeme"/>
		</font>
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      </nobr>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:if test="@kind!='EOF'">
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		<font style="font-family:helvetica" color="F50A19">
		  <xsl:text>LEXEME</xsl:text>
		</font>
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      </xsl:if>
	    </xsl:otherwise>
	  </xsl:choose>
	</td>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="location">
  <nobr>
    <font style="font-family:helvetica">
      <xsl:value-of select="@begline"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="@begcolumn"/>
      <xsl:text>-</xsl:text>
      <xsl:value-of select="@endline"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="@endcolumn"/>
    </font>
  </nobr>
</xsl:template>

</xsl:stylesheet>
