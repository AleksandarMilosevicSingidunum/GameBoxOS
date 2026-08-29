using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using GameBox.Windows.Core;
using Microsoft.Win32;

namespace GameBox.Windows;

public partial class MainWindow : Window
{
    private readonly ObservableCollection<GameEntry> _allGames = new();
    private readonly ObservableCollection<GameEntry> _visibleGames = new();
    private readonly LibraryStore _store;

    public MainWindow()
    {
        InitializeComponent();
        GamesList.ItemsSource = _visibleGames;
        var dataPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "GameBoxOS", "windows-library.json");
        _store = new LibraryStore(dataPath);
        Loaded += async (_, _) => await LoadLibraryAsync();
        PreviewKeyDown += MainWindow_PreviewKeyDown;
    }

    private GameEntry? Selected => GamesList.SelectedItem as GameEntry;

    private async Task LoadLibraryAsync()
    {
        try
        {
            foreach (var game in await _store.LoadAsync()) _allGames.Add(game);
            RefreshVisibleGames();
            StatusText.Text = _allGames.Count + " local game(s)";
        }
        catch (Exception ex)
        {
            StatusText.Text = "Library could not be loaded";
            MessageBox.Show(ex.Message, "GameBox", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
    }

    private async Task SaveLibraryAsync()
    {
        await _store.SaveAsync(_allGames);
        StatusText.Text = _allGames.Count + " local game(s) saved";
    }

    private void RefreshVisibleGames()
    {
        var selectedId = Selected?.Id;
        _visibleGames.Clear();
        foreach (var game in GameLibrary.Filter(_allGames, SearchBox.Text, FavoritesOnlyCheck.IsChecked == true)) _visibleGames.Add(game);
        GamesList.SelectedItem = _visibleGames.FirstOrDefault(x => x.Id == selectedId);
    }

    private async void AddGame_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog { Title = "Add a local Windows game", Filter = "Launchable files (*.exe;*.lnk;*.bat;*.cmd)|*.exe;*.lnk;*.bat;*.cmd", CheckFileExists = true };
        if (dialog.ShowDialog(this) != true) return;
        try
        {
            var game = GameLibrary.Create(Path.GetFileNameWithoutExtension(dialog.FileName), dialog.FileName);
            _allGames.Add(game);
            await SaveLibraryAsync();
            RefreshVisibleGames();
            GamesList.SelectedItem = _visibleGames.FirstOrDefault(x => x.Id == game.Id);
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Unable to add game", MessageBoxButton.OK, MessageBoxImage.Error); }
    }

    private async void Play_Click(object sender, RoutedEventArgs e)
    {
        var game = Selected;
        if (game is null) return;
        try
        {
            var executable = GameLibrary.ValidateExecutablePath(game.ExecutablePath);
            if (!File.Exists(executable)) throw new FileNotFoundException("The configured file is missing.", executable);
            Process.Start(new ProcessStartInfo { FileName = executable, Arguments = game.Arguments, WorkingDirectory = Path.GetDirectoryName(executable) ?? Environment.CurrentDirectory, UseShellExecute = true });
            Replace(game, game with { LastPlayedUtc = DateTimeOffset.UtcNow });
            await SaveLibraryAsync();
            StatusText.Text = "Launched " + game.Title;
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Launch failed", MessageBoxButton.OK, MessageBoxImage.Error); }
    }

    private async void Favorite_Click(object sender, RoutedEventArgs e)
    {
        var game = Selected;
        if (game is null) return;
        Replace(game, game with { Favorite = !game.Favorite });
        await SaveLibraryAsync();
    }

    private async void Remove_Click(object sender, RoutedEventArgs e)
    {
        var game = Selected;
        if (game is null) return;
        if (MessageBox.Show("Remove " + game.Title + " from the companion library? No game files will be deleted.", "Remove library entry", MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
        _allGames.Remove(game);
        await SaveLibraryAsync();
        RefreshVisibleGames();
    }

    private void Replace(GameEntry oldGame, GameEntry newGame)
    {
        var index = _allGames.IndexOf(oldGame);
        if (index >= 0) _allGames[index] = newGame;
        RefreshVisibleGames();
        GamesList.SelectedItem = _visibleGames.FirstOrDefault(x => x.Id == newGame.Id);
    }

    private void GamesList_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        var game = Selected;
        SelectedTitle.Text = game?.Title ?? "Choose a game";
        SelectedPath.Text = game is null ? "" : game.Platform + Environment.NewLine + game.ExecutablePath;
        PlayButton.IsEnabled = game is not null;
        FavoriteButton.IsEnabled = game is not null;
        RemoveButton.IsEnabled = game is not null;
    }

    private void SearchBox_TextChanged(object sender, TextChangedEventArgs e) => RefreshVisibleGames();
    private void FilterChanged(object sender, RoutedEventArgs e) => RefreshVisibleGames();

    private void MainWindow_PreviewKeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.F && Keyboard.Modifiers.HasFlag(ModifierKeys.Control)) { SearchBox.Focus(); SearchBox.SelectAll(); e.Handled = true; }
        else if (e.Key == Key.O && Keyboard.Modifiers.HasFlag(ModifierKeys.Control)) { AddGame_Click(sender, e); e.Handled = true; }
        else if (e.Key == Key.Enter && Selected is not null) { Play_Click(sender, e); e.Handled = true; }
    }
}
