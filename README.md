# Minesweeper (JavaFX)

This project uses a simple MVC split:
- Model: `minesweeper.model`
- Controller: `minesweeper.controller`
- View: `src/main/resources/app/hello-view.fxml`

## Run

Use the Maven wrapper if `mvn` is not on PATH:

```powershell
./mvnw -q -DskipTests package
./mvnw javafx:run
```

If you have Maven installed:

```powershell
mvn -q -DskipTests package
mvn javafx:run
```

