# TytanCraft

TytanCraft to autorska gra voxelowa w Javie. Świat 192 × 192 bloki jest generowany w całości przy uruchomieniu. Seed odtwarza tę samą generację terenu i biomów, z jeziorami, dolinami i górskimi szczytami. Temperatura i wilgotność dobierają m.in. piaski, lasy oraz śnieżne powierzchnie. Zależnie od biomu rosną dęby, brzozy, świerki, akacje i wiśnie, każdy gatunek ma własne drewno i liście; na mapie generuje się maksymalnie pięć voxelowych kwiatów.

## Uruchomienie

Wymagane jest JDK 17 lub nowsze oraz środowisko graficzne:

```sh
./run.sh [seed]
```

Skrypt kompiluje źródła i uruchamia grę. Pominięcie opcjonalnego seedu losuje nowy świat.

Aby zbudować uruchamialny JAR:

```sh
sh package.sh
java -jar build/TytanCraft.jar [seed]
```

Testy bez dodatkowych zależności uruchom poleceniem `sh test.sh`.

## Tryb sieciowy

Serwer dedykowany działa bez interfejsu graficznego i przyjmuje do 10 graczy. W katalogu projektu uruchom:

```sh
sh server.sh [seed] [port]
```

Domyślny port to `25570`. Argumenty można pominąć, aby wylosować seed i użyć portu domyślnego. Po zbudowaniu aplikacji można też uruchomić serwer z JAR-a:

```sh
sh package.sh
sh build/server.sh [seed] [port]
```

W lobby gry wpisz adres `host:port` i wybierz **Dołącz do serwera**. Na tym samym komputerze użyj `127.0.0.1:25570`.

### Dostęp z Internetu

- **playit.gg:** utwórz tunel typu **TCP**, ustaw lokalny adres docelowy `127.0.0.1:25570`, uruchom serwer i podaj graczom przydzielony przez playit adres oraz port.
- **Darmowy VPS:** uruchom `sh server.sh [seed] 25570`, a następnie zezwól na ruch **TCP 25570** w firewallu systemu i regułach sieciowych dostawcy VPS. Gracze łączą się z publicznym adresem VPS i portem `25570`.

## Sterowanie

- W lobby ustaw nick i wybierz **Wejdź do świata** lub naciśnij **Enter**
- Zapisane światy wybierzesz w lobby; **F5** zapisuje bieżący świat, a zamknięcie gry wykonuje zapis automatyczny. Pliki trafiają do `~/.tytancraft/worlds`
- W lobby wybierz tryb **Przetrwanie** albo **Kreatywny**; suwaki ustawiają agresywność zwierząt i szybkość czasu od **0×** do **8×** (**0×** zatrzymuje cykl)
- Suwak **zasięgu renderowania** zmienia odległość widoku i mgły
- Cykl dnia i nocy trwa 4 minuty przy **1×**; słońce przesuwa się po niebie, a pogoda zmienia się między słońcem, chmurami i deszczem. Deszcz pada nieregularnie pod chmurami, a krople rozpryskują się na wodzie i blokach
- **WASD** — ruch, **Spacja** — skok, **Shift** — sprint
- W Przetrwaniu wskaźniki serc i głodu są u góry; pod wodą pojawia się tlen. Jedz surowe mięso lub brązowe grzyby klawiszem **R**; czerwone muchomory odbierają zdrowie
- **Lewy przycisk myszy** niszczy blok lub atakuje wskazane zwierzę; po zabiciu zbierasz mięso, owce upuszczają wełnę, a ryby ikrę swojego gatunku. Dwa zwierzęta tego samego gatunku rozmnażają się, gdy są blisko siebie
- W kreatywnym **Spacja/Ctrl** — lot w górę/w dół; bloki są nieograniczone
- W przetrwaniu zbieraj bloki, pilnuj zdrowia i wydobywaj rudy: węgiel, miedź, żelazo, złoto, diamenty, szmaragdy, rubiny i szafiry
- **E** — otwórz plecak: wybierz bloki do 10 miejsc hotbara albo wytwarzaj deski, pochodnie i bloki szmaragdu
- W wodzie **Spacja** wynurza, **Ctrl** pozwala zanurkować, a **W + Spacja** pomaga wyjść na brzeg; wykopany kanał pozwala wodzie płynąć między jeziorami. Ikry z plecaka stawiaj prawym przyciskiem na wodzie, by przywołać dorsza, łososia, rybę tropikalną, szczupaka albo karpia
- W terenie znajdziesz wejścia do proceduralnie generowanych, zaciemnionych jaskiń
- **Drabinę** stawiaj prawym przyciskiem na pionowej ścianie; przy niej **W/Spacja** wspina, a **S/Ctrl** schodzi
- Trzymając **W**, automatycznie wskoczysz na pojedynczy blok na swojej drodze
- W dolinie spotkasz wędrujące owce, lisy, kury, świnie, jelenie i króliki; zwierzęta spadają do wykopów, a kwiaty i grzyby możesz zbierać oraz sadzić z hotbara
- **Mysz** — rozglądanie się; kliknij okno, żeby przejąć kursor
- **Prawy przycisk myszy** — postaw blok
- **1–9, 0** lub kółko myszy — wybierz blok, **Esc** — zwolnij kursor, **F11** — przełącz pełny ekran

Kod gry i generator terenu zostały napisane na potrzeby tego projektu. Ikony hotbara i plecaka są pobierane pojedynczo z API [BlockRender](https://blockrender.dev/) i buforowane w `~/.tytancraft/blockrender`. Niestandardowe rubiny, szafiry oraz ikry szczupaka i karpia korzystają z najbliższych renderów vanilla: redstone, lapis lazuli, pufferfish i dorsza. Grafiki bazują na zasobach Minecraft Java Edition 26.2: Minecraft textures © Mojang Studios. Render by blockrender.dev. TytanCraft nie jest oficjalnym produktem Minecraft ani nie jest powiązany z Mojang lub Microsoft.
