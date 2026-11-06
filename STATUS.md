# STATUS — 中古車在庫管理アプリ（Usedcar）

最終更新: 2026-11-06 15:30 JST（旧セッションがトークン上限で停止したため引継ぎ → ビルド・テスト・起動まで確認済み）

## 1. 現在の状態

- **ビルド成功 / テスト green**: `mvn test` → `Tests run: 13, Failures: 0, Errors: 0` （`CarTest` 6 + `CarControllerIntegrationTest` 7）
- **起動確認済み**: `mvn spring-boot:run` → `http://localhost:8080` で `/` 200, `/api/meta` 200, `/api/cars?size=2` 200, `/api/dashboard` 200（デモデータ投入済み）
- git: `main` ブランチに 4 コミット（初期スナップショット → コンパイル修正 → 検証完了 → UI オーバーレイ修正）
- 静的ファイルはキャッシュ無効（`spring.web.resources.cache.cachecontrol.no-cache: true`）。ただしブラウザ側に残る場合は `Ctrl+F5` で再読み込み
- 構成: Spring Boot 3.4.4 / Java 21 / H2（ファイル DB `./.data/usedcar`）/ Spring Data JPA / 依存なしの素の JS フロント

## 2. 引継ぎで修正した不具合（5 件）

| # | 箇所 | 症状 | 修正 |
|---|---|---|---|
| 1 | `config/DemoDataSeeder.java` `price(...)` | 第 5 引数 `Long` に int リテラルを渡して 24 箇所でコンパイルエラー | 引数を `long salePrice` に変更 |
| 2 | `service/CarService.java` 並べ替え | `Sort.Order.nulls(...)` は存在せず、`nullsLast()` は JPA Criteria で `UnsupportedOperationException: Applying Null Precedence using Criteria Queries is not yet supported`（一覧 API が 500） | `Sort.by(direction, resolved)` のみに（NULL の扱いは DB 既定に委ねる） |
| 3 | `service/CarService.java` `meta()` | `Map<String, Object> staff = new LinkedHashSet<>()` で型が壊れていた | `java.util.Set<String> staff` に修正 |
| 4 | `web/ApiExceptionHandler.java` | `ObjectError.getPropertyName()` は存在しない → `getObjectName()` に変更。**`@ExceptionHandler` の重複マッピング**（`handleNotFound(RuntimeException)` と `handleJpaNotFound(EntityNotFoundException)`）で ApplicationContext 自体が起動失敗していた | `handleNotFound` は `NotFoundException` のみを処理するよう分離 |
| 5 | `dto/CarRequest.java` のクラスレベル制約 | `@AssertTrue` が getter でないメソッド名（`hasStockDate()` 等）に付いており、Hibernate Validator に無視されていた（納車日 < 契約日 などが素通し） | `isStockDateProvided()` / `isDeliveryAfterContract()` / `isTransferAfterContract()` / `isDeliveryAfterTransfer()` / `isSalePriceRequiredForSale()` / `isScheduleNotAfterStock()` にリネーム |
| 6 | `static/css/app.css` の `.modal-root` / `.drawer` | `hidden` 属性を持つ要素に author の `display: grid` / `display: flex` が優先適用され、**画面全面的な暗いぼかしオーバーレイ（`backdrop-filter: blur(4px)`, `z-index: 60`）と空の詳細ドロワーが常時表示** → 全画面がぼやけて操作不能 | `.is-hidden` の隣に `[hidden] { display: none !important; }` を追加（`app.css:127`） |

追加: `service/DashboardService.java` の `c.stockDate()` → `c.getStockDate()`、`Map.Entry.<String, List<Car>>comparingByValue(bySize)` の型指定。

## 3. テスト内容（`src/test/java/com/usedcar/web/CarControllerIntegrationTest.java`）

MockMvc 統合テスト 7 件: 登録時の原価合計・粗利導出（957,000.00 / 493,000.00）、車台番号空の 422 + fieldErrors、クラスレベル制約（納車日が契約日より前）の 422、車台番号重複の 409、`status` フィルタ + ページング、存在しない ID の 404、PUT → DELETE → 404 のライフサイクル。

テスト DB は `src/test/resources/application.yml`（`jdbc:h2:mem:usedcar-test`、`app.demo-data.enabled: false`）。**ロールバックしない**ため、車台番号はテストごとに一意（`TEST-*`）にしている。

## 4. コマンド（`mvn` は PATH に無い）

```powershell
# .mvn/maven.config に -Dmaven.repo.local=.m2repo と -B 済み
& 'C:\Users\evils\.dsh\tools\apache-maven-3.9.16\bin\mvn.cmd' test
& 'C:\Users\evils\.dsh\tools\apache-maven-3.9.16\bin\mvn.cmd' spring-boot:run   # http://localhost:8080
```

- PowerShell のコマンドラインで `-Dmaven.repo.local=...` を直接渡すと引数が分割されて壊れる。`.mvn/maven.config` 経由のみ使うこと。
- Maven 配布先 `C:\Users\evils\.dsh\tools\apache-maven-3.9.16` はワークスペース外。権限モードが `workspace-write` だとビルドが拒否される（`danger-full-access` が必要）。
- Windows コンソールで javac の日本語エラーが文字化けする場合は `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8` と `$env:JAVA_TOOL_OPTIONS='-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dfile.encoding=UTF-8'` を先に設定する。
- `.m2repo/`（約 45 MB）はテスト依存のダウンロード後に更新済み。`target/`, `.data/`, `*.mv.db` は git 対象外。

## 5. 残っている任意の作業（未着手で問題ない範囲）

- UI の動作確認は HTTP レベルのみ（ブラウザでの目視確認は未実施）
- `CarExportService`（CSV 出力）の統合テストは未作成
- ダッシュボード集計の単体テストは未作成

## 6. 前セッションが止まった理由と、それを避けるルール

ログ解析の結果（セッション `session-74be1f7c…`）:

- contextWindow = 1,000,000 に対し使用ピーク 131,064 トークン → **コンテキスト窓は原因ではない**
- 両ターンの終了理由が `{"kind":"max-tokens"}` → **1 ステップあたりの出力トークン上限**で中絶
- 重いステップの例: 出力 15,049 トークン（reasoning 50,880 字）/ 12,309 / 10,458
- `/compact` は `summarization truncated at the token cap (incomplete checkpoint)` で失敗 → 圧縮は使えない

続行時のルール:

1. 1 ステップで 1 ファイルだけ編集し、1 ファイル 200 行以内（大きく書くなら `write` で骨格 → `edit` で節単位に追記）
2. 複数ファイルの一括書き込みをしない
3. reasoning を短く保つ（effort を下げて開始する）
4. 1 turn では 1 課題だけ扱う（`続けて` だけで投げない）
5. `/compact` は使わない（同じ上限で失敗する）。状態の継続はこのファイルと git に持たせる
6. 各修正の直後に `mvn test` を回す（このセッションでも、ビルドが通るはずのコードで ApplicationContext 起動失敗や無効なバリデーションが見つかった）
