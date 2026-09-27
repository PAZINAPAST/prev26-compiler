<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="imrgen">
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
	  <xsl:apply-templates select="astnode"/>
	</tr>
      </table>
    </body>
  </html>
</xsl:template>

<xsl:template match="astnode">
  <td>
    <table width="100%">
      <tr bgcolor="DDDDDD">
	<td colspan="1000">
	  <table width="100%" cellspacing="3pt">
	    <xsl:choose>
	      <xsl:when test="@none">
		<tr>
		  <td>
		    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		    <font style="font-family:arial black" color="F50A19">
		      <xsl:text>AST</xsl:text>
		    </font>
		    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		  </td>
		</tr>
	      </xsl:when>
	      <xsl:otherwise>
		<tr>
		  <td>
		    <nobr>
		      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>		    
		      <font style="font-size:75%">A<xsl:value-of select="@id"/>:</font><xsl:text> </xsl:text>
		      <font style="font-family:arial black">
			<xsl:value-of select="@label"/>
		      </font>
		      <xsl:if test="@name!=''">
			<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
			<font style="font-family:helvetica">
			  <xsl:value-of select="@name"/>
			</font>
		      </xsl:if>
		      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		    </nobr>
		  </td>
		</tr>
		<xsl:if test="location">
		  <tr>
		    <td>
		      <xsl:apply-templates select="location"/>
		    </td>
		  </tr>
		</xsl:if>
		<xsl:apply-templates select="defat"/>
		<xsl:apply-templates select="istype"/>
		<xsl:apply-templates select="oftype"/>
		<xsl:apply-templates select="isconst"/>
		<xsl:apply-templates select="isaddr"/>
		<xsl:apply-templates select="frame"/>
		<xsl:apply-templates select="access"/>
		<xsl:if test="genimr">
		  <tr bgcolor="A0D4FB">
		    <td>
		      <nobr>
			<font style="font-family:helvetica; font-size:85%">
			  GENERATED IMR:
			</font>
		      </nobr>
		      <br/>
		      <nobr>
			<xsl:apply-templates select="genimr/entrylabel"/>
			<xsl:text> </xsl:text>
			<xsl:apply-templates select="genimr/exitlabel"/>
		      </nobr>
		    </td>
		  </tr>
		</xsl:if>
		<xsl:apply-templates select="imc"/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </table>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="astnode"/>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="exitlabel">
  <xsl:choose>
    <xsl:when test="@label!=''">
      exit-label=<font style="font-family:courier new"><xsl:value-of select="@label"/></font>
    </xsl:when>
    <xsl:otherwise>
      <font color="FF0000">exit-label</font>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="entrylabel">
  <xsl:choose>
    <xsl:when test="@label!=''">
      entry-label=<font style="font-family:courier new"><xsl:value-of select="@label"/></font>
    </xsl:when>
    <xsl:otherwise>
      <font color="FF0000">entry-label</font>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="imc">
  <td>
    <table width="100%">
      <tr>
	<td bgcolor="C7C232" colspan="1000">
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  <xsl:value-of select="@instruction"/>
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="imc"/>
      </tr>
    </table>
  </td>
</xsl:template>
  
<xsl:template match="defat">
  <tr>
    <td bgcolor="FFCF00">
      <nobr>
	<font style="font-family:helvetica; font-size:75%">
	  <xsl:text>defAt: </xsl:text>
	</font>
	<xsl:choose>
	  <xsl:when test="@none">
	    <font style="font-family:arial black" color="F50A19">
	      <xsl:text>LOCATION</xsl:text>
	    </font>
	  </xsl:when>
	  <xsl:otherwise>
	    <font style="font-size:75%">
	      <xsl:text>A</xsl:text>
	      <xsl:value-of select="@id"/>
	      <xsl:text> @ </xsl:text>
	    </font>
	    <xsl:text> </xsl:text>
	    <xsl:apply-templates select="location"/>
	  </xsl:otherwise>
	</xsl:choose>
      </nobr>
    </td>
  </tr>
</xsl:template>

<xsl:template match="istype">
  <tr>
    <td bgcolor="FFCF00">
      <table width="100%">
	<tr>
	  <td align="center">
	    <font style="font-family:helvetica; font-size:75%">
	      <xsl:text>isType:</xsl:text>
	    </font>
	  </td>
	</tr>
	<xsl:choose>
	  <xsl:when test="@none">
	    <tr>
	      <td>
		<font style="font-family:arial black" color="F50A19">
		  <xsl:text>TYPE</xsl:text>
		</font>
	      </td>
	    </tr>
	  </xsl:when>
	  <xsl:otherwise>
	    <tr>
	      <xsl:apply-templates select="typnode"/>
	    </tr>
	  </xsl:otherwise>
	</xsl:choose>
      </table>
    </td>
  </tr>
</xsl:template>

<xsl:template match="oftype">
  <tr>
    <td bgcolor="FFCF00">
      <table width="100%">
	<tr>
	  <td align="center">
	    <font style="font-family:helvetica; font-size:75%">
	      <xsl:text>ofType:</xsl:text>
	    </font>
	  </td>
	</tr>
	<xsl:choose>
	  <xsl:when test="@none">
	    <tr>
	      <td>
		<font style="font-family:arial black" color="F50A19">
		  <xsl:text>TYPE</xsl:text>
		</font>
	      </td>
	    </tr>
	  </xsl:when>
	  <xsl:otherwise>
	    <tr>
	      <xsl:apply-templates select="typnode"/>
	    </tr>
	  </xsl:otherwise>
	</xsl:choose>
      </table>
    </td>
  </tr>
</xsl:template>

<xsl:template match="typnode">
  <td align="center">
    <div align="center">
      <table cellspacing="1pt">
	<tr bgcolor="FFAF11">
	  <td colspan="1000">
	    <xsl:choose>
	      <xsl:when test="@none">
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		<font style="font-family:arial black" color="FF0000">
		  TYPE
		</font>
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      </xsl:when>
	      <xsl:otherwise>
		<nobr>
		  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		  <xsl:if test="@id!=''">
		    <font style="font-size:75%">T<xsl:value-of select="@id"/>:</font><xsl:text> </xsl:text>
		    <xsl:value-of select="@label"/>
		  </xsl:if>
		  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		</nobr>
	      </xsl:otherwise>
	    </xsl:choose>
	  </td>
	</tr>
	<xsl:if test="typnode">
	  <tr>
	    <xsl:apply-templates select="typnode"/>
	  </tr>
	</xsl:if>
      </table>
    </div>
  </td>
</xsl:template>

<xsl:template match="isconst">
  <xsl:choose>
    <xsl:when test="@value='none'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:arial black" color="F50A19">
	    <xsl:text>ISCONST</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
    <xsl:when test="@value='true'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:helvetica; font-size:75%">
	    <xsl:text>isConst</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="isaddr">
  <xsl:choose>
    <xsl:when test="@value='none'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:arial black" color="F50A19">
	    <xsl:text>ISADDR</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
    <xsl:when test="@value='true'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:helvetica; font-size:75%">
	    <xsl:text>isAddr</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="frame">
  <xsl:choose>
    <xsl:when test="@none">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:arial black" color="F50A19">
	    <xsl:text>FRAME</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
    <xsl:otherwise>
      <tr>
	<td bgcolor="DEDC3A">
	  <font style="font-family:helvetica; font-size:85%">
	    <font style="font-family:helvetica; font-size:75%">
	      <xsl:text>FRAME:</xsl:text>
	    </font>
	    <br/>
	    <nobr>
	      label=<font style="font-family:courier new"><xsl:value-of select="@label"/></font>
	      depth=<xsl:value-of select="@depth"/> 
	      size=<xsl:value-of select="@size"/> 
	      locs=<xsl:value-of select="@locssize"/>
	      args=<xsl:value-of select="@argssize"/>
	      FP=<xsl:value-of select="@FP"/>
	      RV=<xsl:value-of select="@RV"/>
	    </nobr>
	  </font>
	</td>
      </tr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="access">
  <xsl:choose>
    <xsl:when test="@none">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:arial black" color="F50A19">
	    <xsl:text>ACCESS</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
    <xsl:otherwise>
      <tr>
	<td bgcolor="DEDC3A">
	  <font style="font-family:helvetica; font-size:85%">
	    <font style="font-family:helvetica; font-size:75%">
	      <xsl:text>ACCESS:</xsl:text>
	    </font>
	    <br/>
	    <nobr>
	      <xsl:if test="@label!=''">
		label=<font style="font-family:courier new"><xsl:value-of select="@label"/></font>
	      </xsl:if>
	      <xsl:if test="@offset!=''">
		offset=<xsl:value-of select="@offset"/>
	      </xsl:if>
	      size=<xsl:value-of select="@size"/> 
	      <xsl:if test="@depth!=''">
		depth=<xsl:value-of select="@depth"/>
	      </xsl:if>
	      <xsl:if test="@init!=''">
		init=<font style="font-family:courier new"><xsl:value-of select="@init"/></font>
	      </xsl:if>
	    </nobr>
	  </font>
	</td>
      </tr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="location">
  <xsl:choose>
    <xsl:when test="@none">
      <font style="font-family:helvetica" color="F50A19">
	<xsl:text>LOCATION</xsl:text>
      </font>
    </xsl:when>
    <xsl:when test="(@begline='100') or (@begcolumn='100') or (@endline='100') or (@endcolumn='100')">
      <font style="font-family:helvetica" color="F50A19">
	<xsl:text>LOCATION</xsl:text>
      </font>
    </xsl:when>
    <xsl:otherwise>
      <nobr>
	<xsl:choose>
	  <xsl:when test="@begline>0">
	    <xsl:value-of select="@begline"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <font style="font-family:helvetica" color="F50A19">
	      <xsl:text>BegLine</xsl:text>
	    </font>
	  </xsl:otherwise>
	</xsl:choose>
	<xsl:text>:</xsl:text>
	<xsl:choose>
	  <xsl:when test="@begcolumn>0">
	    <xsl:value-of select="@begcolumn"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <font style="font-family:helvetica" color="F50A19">
	      <xsl:text>BegColumn</xsl:text>
	    </font>
	  </xsl:otherwise>
	</xsl:choose>
	<xsl:text>-</xsl:text>
	<xsl:choose>
	  <xsl:when test="@endline>0">
	    <xsl:value-of select="@endline"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <font style="font-family:helvetica" color="F50A19">
	      <xsl:text>EndLine</xsl:text>
	    </font>
	  </xsl:otherwise>
	</xsl:choose>
	<xsl:text>:</xsl:text>
	<xsl:choose>
	  <xsl:when test="@endcolumn>0">
	    <xsl:value-of select="@endcolumn"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <font style="font-family:helvetica" color="F50A19">
	      <xsl:text>EndColumn</xsl:text>
	    </font>
	  </xsl:otherwise>
	</xsl:choose>
      </nobr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
