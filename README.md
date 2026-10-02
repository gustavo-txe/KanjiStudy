# Kanji Scanner

O aplicativo Android nativo Kanji Scanner foi desenvolvido para auxiliar o usuário no estudo e na identificação de kanji de forma simples e prática.  
Nesta aplicação o usuário é capaz de identificar kanji pela câmera ou por imagens da galeria, consultar leituras, significados e acompanhar seu progresso no aprendizado dos 2.136 kanji Jōyō, tudo funcionando de forma totalmente offline após o download.

Desenvolvido por Gustavo Teixeira e publicado na [Google Play](https://play.google.com/store/apps/details?id=com.app.kanjistudy).

## Funcionalidades

**Reconhecimento pela câmera e por imagens**

- OCR de texto japonês utilizando do ML Kit e análise da câmera com CameraX.
- Seleção dos caracteres reconhecidos para consultar detalhes ou marcar como aprendidos.
- Pausa da câmera e seleção de imagens com recorte e ajustes pelo Document Scanner, quando disponível.

![Screenshot scan camera](images/screenshot_camera_scan.jpeg)
![Screenshot scan camera](images/screenshot_camera_scan2.jpeg)
![Screenshot scan imagens](images/screenshot_image_scan.jpeg)

**Consulta ao catálogo**

- Busca por caractere, leitura ou significado e filtro por nível do JLPT.
- Catálogo armazenado no dispositivo após o download inicial, permitindo consultas offline.
- Ações para copiar caracteres e abrir uma pesquisa no Google AI pelo navegador.

![Screenshot pesquisa](images/screenshot_search.jpeg)
![Screenshot detalhes](images/screenshot_kanji_details.jpeg)

**Progresso e preferências**

- Lista de kanji aprendidos com indicador de progresso.
- Persistência local e backup dos kanji aprendidos no formato JSON.
- Exportação e importação pelo seletor de arquivos do Android. A importação valida o conteúdo e mescla os caracteres ao progresso existente.
- Temas claro e escuro com preferência salva e guia de uso dentro do app.

![Screenshot kanji aprendidos](images/screenshot_kanji_learned.jpeg)
![Screenshot menu](images/screenshot_menu.jpeg)

## Stack utilizada neste projeto

| Área | Tecnologias |
| --- | --- |
| Linguagem | Kotlin 2.0.21 |
| Interface | Jetpack Compose (BOM 2024.09.00), Material 3 e AndroidX Lifecycle |
| Navegação | Navigation Compose 2.9.0 |
| Arquitetura | MVVM, Repository Pattern, StateFlow e Flow |
| Concorrência | Kotlin Coroutines 1.9.0, dispatchers injetados e Mutex |
| Persistência | Room 2.8.4 e DataStore Preferences 1.1.1 |
| Injeção de dependências | Dagger Hilt 2.51.1, com KAPT |
| Dados remotos e JSON | Retrofit 2.11.0, converter Gson 2.11.0 e Gson 2.10.1; API kanjiapi.dev |
| Câmera e OCR | CameraX 1.4.0, ML Kit Japanese Text Recognition 16.0.1 e Document Scanner 16.0.0 |
| Imagens | ImageDecoder, BitmapFactory e AndroidX ExifInterface 1.3.2 |
| Integrações Android | Seletor de arquivos, Android Backup, Custom Tabs (Browser 1.8.0) e In-App Review 2.0.2 |
| Testes locais | JUnit 4.13.2, MockK 1.13.13, Turbine 1.2.0, Robolectric 4.14.1, Coroutines Test e Compose UI Test |
| Testes instrumentados | AndroidX Test, Espresso 3.7.0, Compose UI Test e Hilt Testing |
| Diagnóstico | LeakCanary 2.14 nas builds de debug |
| Build | Android Gradle Plugin 8.13.2, Gradle Wrapper 8.14.5 e bytecode Java/Kotlin com alvo JVM 11 |
| Android | Mínimo: Android 8.0 (API 26); compileSdk e targetSdk: API 36 |
| Versão no repositório | 1.2.4 (`versionCode` 15) |

O código está organizado em `presentation` (telas, estados e ViewModels), `domain` (modelos e busca), `data` (banco, API, preferências, OCR e arquivos), `di` (injeção de dependências) e `core` (recursos compartilhados).

Os ViewModels expõem estados observáveis e delegam o acesso aos dados ao repositório. O Room mantém o catálogo e o progresso; as migrações do banco e as operações de importação são testadas. As builds de release utilizam R8 e redução de recursos.

## Testes

A validação do projeto inclui testes de busca, estados dos ViewModels, persistência, migrações do banco, importação e exportação de progresso e processamento de imagens. Os testes instrumentados complementam essa cobertura com fluxos de interface e integração.

Para avaliar o código localmente, estes comandos executam os testes e a análise estática a partir da raiz do projeto, no Windows:

```powershell
# Testes locais
.\gradlew.bat :app:testDebugUnitTest

# Testes instrumentados: emulador ou dispositivo de teste conectado
.\gradlew.bat :app:connectedDebugAndroidTest

# Análise estática
.\gradlew.bat :app:lintDebug
```

## 📫 Contato

**Gustavo Teixeira**  
E-mail: [gustavoteixeira.ggt@gmail.com](mailto:gustavoteixeira.ggt@gmail.com)
