package com.asemicanalytics.cli.semanticlayer.internal.dsgenerator;

import com.asemicanalytics.cli.model.ColumnDto;
import com.asemicanalytics.cli.semanticlayer.internal.GlobalConfig;
import com.asemicanalytics.cli.semanticlayer.internal.QueryEngineClient;
import com.asemicanalytics.cli.semanticlayer.internal.cli.InputCli;
import com.asemicanalytics.cli.semanticlayer.internal.cli.SpinnerCli;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.print.attribute.standard.SheetCollate;

public class DsGeneratorHelper {
  private final QueryEngineClient queryEngineClient;
  private final Optional<Boolean> noWizard;

  public DsGeneratorHelper(QueryEngineClient queryEngineClient, Optional<Boolean> noWizard) {
    this.queryEngineClient = queryEngineClient;
    this.noWizard = noWizard;
  }

  public String recommendedDatasourceName(String table) {
    return Arrays.stream(table.split("\\.")).toList().getLast();
  }

  public String readInput(Optional<String> option, String optionName,
                          Optional<String> header, String prompt,
                          Optional<String> defaultValue) {
    return option.orElseGet(() -> {
      if (noWizard.isPresent() && noWizard.get()) {
        throw new RuntimeException("Missing required option --"
            + optionName + " (or omit --no-wizard flag)");
      }
      header.ifPresent(System.out::println);
      return new InputCli(prompt, defaultValue, s -> true).read();
    });
  }

  public List<ColumnDto> getTableSchema(String table) {
    var columns = new SpinnerCli().spin(() ->
        queryEngineClient.getColumns(GlobalConfig.getAppId(), table));

    System.out.println("Columns:");
    for (var column : columns) {
      System.out.println(
          "    " + column.getId() + " [" + column.getDataType().toUpperCase() + "]");
    }
    return columns;
  }
}
