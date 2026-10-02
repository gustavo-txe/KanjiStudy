# Testes instrumentados

Execute com um emulador ou dispositivo de teste conectado e desbloqueado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Para apenas compilar os APKs, sem dispositivo:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
```

Relatório: `app/build/reports/androidTests/connected/debug/index.html`.

## Cobertura

`AppFlowTest` inicia a `MainActivity` de produção com Hilt e interage com a UI Compose:

- busca, marcação de kanji, navegação para aprendidos, recriação da Activity e remoção;
- mudança de tema e fechamento/reabertura da Activity;
- navegação para ajuda e retorno à busca;
- exportação e importação, verificando o JSON e a mesclagem do progresso;
- cancelamento do seletor de documentos e rejeição de conteúdo inválido.

`PersistenceTest` fecha e reabre Room e DataStore reais. Isso verifica a persistência em disco além da preservação dos ViewModels durante uma recriação da Activity.

## Isolamento e limites

Os testes usam repositórios, ViewModels, banco e preferências reais. `@BindValue` fornece duas instâncias de repositórios com armazenamento exclusivo de cada teste; os demais componentes continuam sendo construídos pelo Hilt. O catálogo local é preenchido com 2136 entradas para satisfazer a condição de download completo. Qualquer tentativa de usar a API externa falha explicitamente.

Espresso Intents controla somente o resultado do seletor do sistema. A leitura e escrita passam pelo `ContentResolver` de produção e por URIs `content://` reais criadas no MediaStore. Esses dois testes exigem API 29 ou superior, pois usam Downloads com armazenamento delimitado e sem permissões de armazenamento. Os demais podem rodar a partir da API mínima do app (26).

Banco, preferências e backup ficam em um diretório temporário. Os documentos do MediaStore são apagados ao final de cada teste. As operações dos testes usam apenas esse armazenamento isolado. A execução instala os APKs de debug e teste, portanto use um dispositivo destinado a testes.

Não se usa `Thread.sleep`: sincronização de Compose e espera limitada por emissões de Room/DataStore controlam a execução. A camada instrumentada complementa `src/test`; não replica os testes unitários de filtros, tentativas de download ou validação de todos os formatos JSON.

Essa suíte não automatiza a interface do seletor de cada fabricante, concessões de URI vindas de outro aplicativo, morte do processo pelo sistema, captura física da câmera, precisão do ML Kit ou interfaces do Google Play. Esses fluxos continuam exigindo validação manual em dispositivo antes de publicar.
