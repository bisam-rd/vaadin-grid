package com.vaadin.components.grid.table;

import static com.google.gwt.query.client.GQuery.$;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.query.client.js.JsUtils;
import com.google.gwt.query.client.plugins.observe.Observe;
import com.google.gwt.query.client.plugins.observe.Observe.Changes.MutationRecord;
import com.google.gwt.query.client.plugins.observe.Observe.MutationListener;
import com.google.gwt.user.client.Timer;
import com.vaadin.client.widgets.Grid;
import com.vaadin.client.widgets.Grid.Column;
import com.vaadin.client.widgets.Grid.StaticSection.StaticCell;
import com.vaadin.client.widgets.Grid.StaticSection.StaticRow;
import com.vaadin.components.common.js.JS;
import com.vaadin.components.common.js.JSArray;
import com.vaadin.components.common.js.JSValidate;
import com.vaadin.components.grid.GridComponent;
import com.vaadin.components.grid.config.JSColumn;
import com.vaadin.components.grid.config.JSStaticCell;
import com.vaadin.components.grid.data.GridDomTableDataSource;

/**
 * This class represents a grid header configuration based on a DOM structure.
 */
public class GridLightDomTable implements MutationListener {

    private String lastConfigString = null;
    private final GQuery $light;
    private GQuery $thead;
    private GQuery $tfoot;
    private GQuery $head_tr;
    private GQuery $foot_tr;
    private GQuery $cols;
    private final Grid<Object> grid;
    private final GridComponent gridComponent;
    int defaultHeaderRow, numberHeaderRows, numberColumns, numberFooterRows;
    private final GridDomTableDataSource ds;

    public GridLightDomTable(Element tableElement, GridComponent grid) {
        this.gridComponent = grid;
        this.grid = grid.getGrid();

        ds = GridDomTableDataSource.createInstance(tableElement, gridComponent);
        $light = $(tableElement);

        parseDom();

        $light.as(Observe.Observe).mutation(
                Observe.createMutationInit().attributes(true).childList(true)
                        .subtree(true), this);

    }

    public void parseDom() {
        $thead = $light.find("thead");
        if ($thead.isEmpty()) {
            $thead = $("<thead>").appendTo($light);
        }
        $tfoot = $light.find("tfoot");
        if ($tfoot.isEmpty()) {
            $tfoot = $("<tfoot>").appendTo($light);
        }
        $cols = $light.find("colgroup");
        String txt = $thead.toString() + $cols.toString() + $tfoot.toString();
        if (!txt.equals(lastConfigString)) {
            lastConfigString = txt;

            $head_tr = $thead.find("tr");
            $foot_tr = $tfoot.find("tr");
            numberHeaderRows = $head_tr.size();
            numberFooterRows = $foot_tr.size();

            // Create the jsColumns array
            configureColumns();

            if (numberColumns > 0) {
                if (numberHeaderRows > 0) {
                    // Configure Headers
                    configureHeadersFooters(true);
                }
                if (numberFooterRows > 0) {
                    // Configure Footers
                    configureHeadersFooters(false);
                }
            }
        }
    }

    private void configureColumns() {
        for (int i = 0, max = 0; i < numberHeaderRows; i++) {
            GQuery $ths = $head_tr.eq(i).children("th, td");
            max = Math.max(max, $ths.size());

            // The default row should be the last row in the header which has
            // the largest number of th elements, or the last header row which
            // contains a th element with the sortable attribute
            if ($ths.size() == max || !$ths.filter("[sortable]").isEmpty()) {
                defaultHeaderRow = i;
            }
        }

        if (!$cols.isEmpty()) {
            configureColumnsFromDom($cols.children("col"));
        } else {
            configureColumnsFromDom($head_tr.eq(defaultHeaderRow)
                    .children("th"));
        }
    }

    private void configureColumnsFromDom(GQuery $ths) {
        JSArray<JSColumn> jsColumns = gridComponent.getColumns();
        jsColumns.setLength(0);
        numberColumns = $ths.size();
        for (int i = 0; i < numberColumns; i++) {
            GQuery $th = $ths.eq(i);
            JSColumn column = JS.createJsType(JSColumn.class);
            jsColumns.add(column);
            column.setSortable(JSValidate.Boolean.attr($th, "sortable"));
            column.setReadOnly(JSValidate.Boolean.attr($th, "read-only"));
            column.setFlex(JSValidate.Integer.attr($th, "flex", 0, 1));

            Double width = JSValidate.Pixel.attr($th, "width");
            if (width != null) {
                column.setWidth(width);
            }
            Double minWidth = JSValidate.Pixel.attr($th, "min-width");
            if (minWidth != null) {
                column.setMinWidth(minWidth);
            }
            Double maxWidth = JSValidate.Pixel.attr($th, "max-width");
            if (maxWidth != null) {
                column.setMaxWidth(maxWidth);
            }

            column.setName(JSValidate.String.attr($th, "name"));
            column.setRenderer(JsUtils.wrapFunction(new Function() {
                @Override
                public void f() {
                    JavaScriptObject cell = arguments(0);
                    Element element = JsUtils.prop(cell, "element");
                    Object data = JsUtils.prop(cell, "data");
                    element.setInnerHTML(data != null ? String.valueOf(data)
                            : "");
                }
            }));
            column.setHeaderContent($th.html());
        }
        gridComponent.setColumns(jsColumns);
    }

    private void configureHeadersFooters(final boolean isHeader) {
        int n = isHeader ? grid.getHeaderRowCount() : grid.getFooterRowCount();
        int nrows = isHeader ? numberHeaderRows : numberFooterRows;
        GQuery $rows = isHeader ? $head_tr : $foot_tr;
        if (n != nrows) {
            for (int i = nrows; i < n; i++) {
                if (isHeader) {
                    grid.removeHeaderRow(i);
                } else {
                    grid.removeFooterRow(i);
                }
            }
            for (int i = n; i < nrows; i++) {
                if (isHeader) {
                    grid.addHeaderRowAt(i);
                } else {
                    grid.addFooterRowAt(i);
                }
            }
        }
        for (int i = 0; i < nrows; i++) {
            StaticRow<?> row = isHeader ? grid.getHeaderRow(i) : grid
                    .getFooterRow(i);
            List<Column<?, Object>> dataColumns = gridComponent
                    .getDataColumns();
            GQuery $tr = $rows.eq(i);
            GQuery $ths = $tr.children("th, td");
            String className = JSValidate.String.attr($tr, "class");
            if (!className.isEmpty()) {
                row.setStyleName(className);
            }

            for (int j = 0; j < $ths.size() && j < numberColumns; j++) {
                final GQuery $th = $ths.eq(j);
                Column<?, Object> column = dataColumns.get(j);
                StaticCell cell = row.getCell(column);
                JSStaticCell js = gridComponent.getStaticSection()
                        .assureJSStaticCell(cell);
                js.setContent($th.html());
                // TODO: for some reason this not work without a timeout
                new Timer() {
                    @Override
                    public void run() {
                        js.setColspan(JSValidate.Integer.attr($th, "colspan",
                                0, 1));
                    }
                }.schedule(0);
                className = JSValidate.String.attr($th, "class");
                if (!className.isEmpty()) {
                    js.setClassName(className);
                }
            }
        }
        // TODO: remove timer when #17327 is fixed
        new Timer() {
            @Override
            public void run() {
                if (isHeader) {
                    gridComponent.getStaticSection().setDefaultHeader(
                            defaultHeaderRow);
                    grid.setHeaderVisible(!(boolean) JSValidate.Boolean.attr(
                            $thead, "hidden"));
                } else {
                    if (!$foot_tr.isEmpty()) {
                        grid.setFooterVisible(!(boolean) JSValidate.Boolean
                                .attr($tfoot, "hidden"));
                    }
                }
            }
        }.schedule(0);
    }

    public GridDomTableDataSource getDomDataSource() {
        return ds;
    }

    @Override
    public void onMutation(List<MutationRecord> mutations) {
        boolean dataChanged = false;
        boolean headerChanged = false;
        for (MutationRecord r : mutations) {
            GQuery g = $(r.removedNodes()).add($(r.addedNodes()))
            // Find the parents of the mutated nodes
                    .closest("tbody,thead,tfoot");
            if (!headerChanged
                    && (!g.filter("thead,tfoot").isEmpty() || r.attributeName() != null)) {
                headerChanged = true;
            }
            if (!dataChanged && !g.filter("tbody").isEmpty()) {
                dataChanged = true;
            }
        }

        if (headerChanged || !dataChanged) {
            parseDom();
            gridComponent.redraw(true);
        }
        if (dataChanged) {
            gridComponent.refresh();
        }
    }
}
