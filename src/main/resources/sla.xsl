<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
    <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>

    <xsl:template match="bug-reports">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="simpleA4" page-height="29.7cm" page-width="21cm" margin-top="2cm" margin-bottom="1cm" margin-left="2cm" margin-right="1cm">
                    <fo:region-body region-name="xsl-region-body" margin="0.5cm" margin-top="3cm"/>
                    <fo:region-before region-name="xsl-region-before" margin="0.5cm" extent="3cm" display-align="before" />
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="simpleA4">
                <xsl:apply-templates select="report-header"/>
                <fo:flow flow-name="xsl-region-body">
                    <fo:block/>
                    <xsl:apply-templates select="out-sla-bugs"/>
                </fo:flow>
            </fo:page-sequence>

        </fo:root>
    </xsl:template>

    <xsl:template match="report-header">
        <fo:static-content flow-name="xsl-region-before">
            <fo:block font-size="12pt" font-weight="bold" text-align="center" space-after="5mm" color="white" background-color="#365f91" border-color="black" border-width="1pt" border-style="solid">
                <fo:block>SLA Report, Week</fo:block>
                <fo:block>#<xsl:value-of select="format-dateTime(date, '[W]')"/></fo:block>
                <fo:block><xsl:value-of select="format-dateTime(date, '[D1o] [MNn], [Y]', 'en', (), ())"/></fo:block>
                <fo:block text-align="outside">
                    Page <fo:page-number font-style="normal" /> of <fo:page-number-citation ref-id='end'/>
                </fo:block>
            </fo:block>
        </fo:static-content>
    </xsl:template>

    <xsl:template match="out-sla-bugs">
        <fo:block font-size="12pt" font-weight="bold">P1/P2 out of SLA</fo:block>
        <fo:block font-size="10pt">
            <fo:table width="100%" border-collapse="collapse">
                <fo:table-header color="white" background-color="#5b9bd5">
                    <fo:table-cell border="solid .5px black" padding="1em" width="3cm">
                        <fo:block text-align="justify" font-weight="bold">Priority</fo:block>
                    </fo:table-cell>
                    <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
                        <fo:block text-align="justify" font-weight="bold">Bug number</fo:block>
                    </fo:table-cell>
                </fo:table-header>
                <fo:table-body>
                    <xsl:apply-templates select="number-of-bugs"/>
                </fo:table-body>
            </fo:table>
            <fo:block font-size="8pt" text-align="right">* As of <xsl:value-of select="format-dateTime(//report-header/date, '[FNn], [D]-[MN,*-3]-[Y], [h]:[m01][PN] [z]', 'en', (), ())"/></fo:block>
            <fo:block font-size="8pt" text-align="right">** Excluding CRF Hot Deploys, EComm Hot Deploys, `Dataload Failed`, `New Files Arrived` and `Data Consistency` Reports</fo:block>
        </fo:block>
        <!--<fo:block text-align="center">-->
            <!--<fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>-->
        <!--</fo:block>-->
    </xsl:template>

    <xsl:template match="out-sla-bugs/number-of-bugs">
        <fo:table-row>
            <xsl:choose>
                <xsl:when test="priority = 'P1'"><xsl:attribute name="color">red</xsl:attribute></xsl:when>
                <xsl:when test="priority = 'P2'"><xsl:attribute name="color">blue</xsl:attribute></xsl:when>
                <xsl:when test="priority = 'Grand Total'"><xsl:attribute name="font-weight">bold</xsl:attribute></xsl:when>
            </xsl:choose>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="priority"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
                <fo:block>
                    <xsl:value-of select="number"/>
                </fo:block>
            </fo:table-cell>
        </fo:table-row>
    </xsl:template>

</xsl:stylesheet>