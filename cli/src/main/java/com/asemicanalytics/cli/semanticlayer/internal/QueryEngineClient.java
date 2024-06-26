package com.asemicanalytics.cli.semanticlayer.internal;

import com.asemicanalytics.cli.model.ChartDataDto;
import com.asemicanalytics.cli.model.ChartRequestDto;
import com.asemicanalytics.cli.model.ColumnDto;
import com.asemicanalytics.cli.model.DatabaseDto;
import com.asemicanalytics.cli.model.DatasourceDto;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.runtime.ApplicationConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class QueryEngineClient {
  private final BlockingHttpClient httpClient;

  @Inject
  public QueryEngineClient() {
    HttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
    configuration.setReadTimeout(Duration.ofSeconds(300));
    this.httpClient = new DefaultHttpClient((URI) null, configuration).toBlocking();
  }

  public List<ColumnDto> getColumns(String appId, String table) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("datasources")
        .path(table)
        .path("columns")
        .build();

    HttpRequest<?> request = HttpRequest.GET(uri)
        .bearerAuth(GlobalConfig.getApiToken());

    try {
      return httpClient.retrieve(request, Argument.listOf(ColumnDto.class));
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }
  }

  public Map<String, DatasourceDto> getDailyDatasources(String appId, Optional<String> version) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("datasources/daily")
        .build();

    MutableHttpRequest<?> request = HttpRequest.GET(uri)
        .bearerAuth(GlobalConfig.getApiToken());
    version.ifPresent(v -> request.header("AppConfigVersion", v));

    try {
      return httpClient.retrieve(request, Argument.mapOf(String.class, DatasourceDto.class));
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }
  }

  public ChartDataDto submitChart(String appId, ChartRequestDto chartRequestDto,
                                  Optional<String> version) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("charts/submit")
        .build();

    MutableHttpRequest<?> request = HttpRequest.POST(uri, chartRequestDto)
        .bearerAuth(GlobalConfig.getApiToken())
        .contentType(MediaType.APPLICATION_JSON);
    version.ifPresent(v -> request.header("AppConfigVersion", v));

    try {
      return httpClient.retrieve(request, ChartDataDto.class);
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }
  }

  public void submitDbAuth(String appId, DatabaseDto databaseDto) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("datasources-configure")
        .path("db-auth")
        .build();

    HttpRequest<?> request = HttpRequest.POST(uri, databaseDto)
        .bearerAuth(GlobalConfig.getApiToken())
        .contentType(MediaType.APPLICATION_JSON);

    try {
      httpClient.retrieve(request, String.class);
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }
  }

  public void downloadCurrentConfig(String appId, Path configPath) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("datasources/current-config")
        .build();

    MutableHttpRequest<?> request = HttpRequest.GET(uri)
        .bearerAuth(GlobalConfig.getApiToken())
        .accept(MediaType.APPLICATION_OCTET_STREAM);

    final byte[] response;
    try {
      response = httpClient.retrieve(request, byte[].class);
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }

    try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
      fos.write(response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void downloadConfigByVersion(String appId, String version, Path configPath) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("datasources/config")
        .path(version)
        .build();

    MutableHttpRequest<?> request = HttpRequest.GET(uri)
        .bearerAuth(GlobalConfig.getApiToken())
        .accept(MediaType.APPLICATION_OCTET_STREAM);

    final byte[] response;
    try {
      response = httpClient.retrieve(request, byte[].class);
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }

    try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
      fos.write(response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void uploadConfig(String appId, Path configPath, Optional<String> version) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("datasources-configure/config")
        .build();

    File file = configPath.toFile();

    MultipartBody body = MultipartBody.builder()
        .addPart("appConfig", file.getName(), MediaType.MULTIPART_FORM_DATA_TYPE, file)
        .build();

    MutableHttpRequest<?> request = HttpRequest.POST(uri, body)
        .bearerAuth(GlobalConfig.getApiToken())
        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE);
    version.ifPresent(v -> request.header("AppConfigVersion", v));

    try {
      httpClient.exchange(request);
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }
    System.out.println("Config uploaded successfully");
  }

  public void backfillUserWide(String appId, LocalDate date, Optional<String> version) {
    var uri = UriBuilder.of(GlobalConfig.getApiUri())
        .path("api/v1")
        .path(appId)
        .path("datasources/backfill-userwide")
        .path(date.toString())
        .build();

    MutableHttpRequest<?> request = HttpRequest.POST(uri, null)
        .bearerAuth(GlobalConfig.getApiToken())
        .contentType(MediaType.APPLICATION_JSON);
    version.ifPresent(v -> request.header("AppConfigVersion", v));

    try {
      httpClient.exchange(request);
    } catch (Exception e) {
      throw new QueryEngineException(e.getMessage());
    }
  }
}
