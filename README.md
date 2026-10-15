# TytanCraft

TytanCraft to autorska gra voxelowa w Javie. Świat 192 × 192 bloki jest generowany w całości przy uruchomieniu. Seed odtwarza tę samą generację terenu i biomów, z jeziorami, dolinami i górskimi szczytami. Temperatura i wilgotność dobierają m.in. piaski, lasy oraz śnieżne powierzchnie. Zależnie od biomu rosną dęby, brzozy, świerki, akacje i wiśnie, każdy gatunek ma własne drewno i liście; na mapie generuje się maksymalnie pięć voxelowych kwiatów.

## Uruchomienie

Wymagane jest JDK 17 lub nowsze oraz środowisko graficzne:

```sh
./run.sh [seed]
```

Skrypt kompiluje źródła i uruchamia grę. Pominięcie opcjonalnego seedu losuje nowy świat.

## Sterowanie

- W lobby ustaw nick i wybierz **Wejdź do świata** lub naciśnij **Enter**
- W lobby wybierz tryb **Przetrwanie** albo **Kreatywny**; suwaki ustawiają agresywność zwierząt i szybkość czasu od **0×** do **8×** (**0×** zatrzymuje cykl)
- Suwak **zasięgu renderowania** zmienia odległość widoku i mgły
- Cykl dnia i nocy trwa 4 minuty przy **1×**; słońce przesuwa się po niebie, a pogoda zmienia się między słońcem, chmurami i deszczem
- **WASD** — ruch, **Spacja** — skok, **Shift** — sprint
- W Przetrwaniu wskaźniki serc i głodu są u góry; pod wodą pojawia się tlen. Jedz surowe mięso lub brązowe grzyby klawiszem **R**; czerwone muchomory odbierają zdrowie
- **Lewy przycisk myszy** atakuje wskazane zwierzę; po zabiciu zbierasz mięso, owce upuszczają wełnę, a ryby ikrę swojego gatunku. Dwa zwierzęta tego samego gatunku rozmnażają się, gdy są blisko siebie
- W kreatywnym **Spacja/Ctrl** — lot w górę/w dół; bloki są nieograniczone
- W przetrwaniu zbieraj bloki, pilnuj zdrowia i wydobywaj rudy: węgiel, miedź, żelazo, złoto, diamenty, szmaragdy, rubiny i szafiry
- **E** — otwórz plecak: wybierz bloki do 10 miejsc hotbara albo wytwarzaj deski, pochodnie i bloki szmaragdu
- W wodzie **Spacja** wynurza, **Ctrl** pozwala zanurkować, a **W + Spacja** pomaga wyjść na brzeg; wykopany kanał pozwala wodzie płynąć między jeziorami. Ikry z plecaka stawiaj prawym przyciskiem na wodzie, by przywołać dorsza, łososia, rybę tropikalną, szczupaka albo karpia
- W terenie znajdziesz wejścia do proceduralnie generowanych, zaciemnionych jaskiń
- Trzymając **W**, automatycznie wskoczysz na pojedynczy blok na swojej drodze
- W dolinie spotkasz wędrujące owce, lisy, kury, świnie, jelenie i króliki; zwierzęta spadają do wykopów, a kwiaty i grzyby możesz zbierać oraz sadzić z hotbara
- **Mysz** — rozglądanie się; kliknij okno, żeby przejąć kursor
- **Lewy przycisk myszy** — zniszcz blok lub zaatakuj zwierzę, **prawy** — postaw blok
- **1–9, 0** lub kółko myszy — wybierz blok, **Esc** — zwolnij kursor

## Opcjonalny moduł C++

Opcjonalna biblioteka C++/JNI w `native/` przyspiesza generowanie mapy wysokości; bez niej używany jest zgodny generator w Javie.

```sh
cmake -S native -B native/build
cmake --build native/build
mkdir -p build/classes
javac -encoding UTF-8 -d build/classes src/main/java/dev/tytancraft/*.java
java -Djava.library.path=native/build -cp build/classes dev.tytancraft.TytanCraft
```

Kod gry i generator terenu zostały napisane na potrzeby tego projektu. Nazwa projektu jest autorska; nie użyto kodu, tekstur ani modeli z Minecrafta ani innych cudzych zasobów.
# TYTANCRAFT
