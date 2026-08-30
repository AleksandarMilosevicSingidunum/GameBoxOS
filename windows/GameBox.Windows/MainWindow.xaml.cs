using System.IO;
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
            RefreshPlatformOptions();
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
        var sort = SortBox.SelectedIndex == 1 ? LibrarySort.RecentlyPlayed : LibrarySort.FavoritesThenTitle;
        var platform = PlatformBox.SelectedIndex <= 0 ? null : PlatformBox.SelectedItem as string;
        foreach (var game in GameLibrary.Filter(_allGames, SearchBox.Text, FavoritesOnlyCheck.IsChecked == true, AvailableOnlyCheck.IsChecked == true, platform, sort)) _visibleGames.Add(game);
        GamesList.SelectedItem = _visibleGames.FirstOrDefault(x => x.Id == selectedId);
    }

    private void RefreshPlatformOptions()
    {
        var selected = PlatformBox.SelectedItem as string;
        PlatformBox.Items.Clear();
        PlatformBox.Items.Add("All platforms");
        foreach (var platform in _allGames.Select(x => x.Platform).Distinct(StringComparer.CurrentCultureIgnoreCase).OrderBy(x => x, StringComparer.CurrentCultureIgnoreCase))
            PlatformBox.Items.Add(platform);
        PlatformBox.SelectedItem = selected is not null && PlatformBox.Items.Contains(selected) ? selected : "All platforms";
    }

    private async void BackupLibrary_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new SaveFileDialog { Title = "Back up GameBox library", Filter = "GameBox library backup (*.json)|*.json", FileName = "gamebox-windows-library.json", AddExtension = true };
        if (dialog.ShowDialog(this) != true) return;
        try
        {
            await _store.ExportAsync(dialog.FileName);
            StatusText.Text = "Library backup created";
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Backup failed", MessageBoxButton.OK, MessageBoxImage.Error); }
    }

    private async void RestoreLibrary_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog { Title = "Restore GameBox library", Filter = "GameBox library backup (*.json)|*.json", CheckFileExists = true };
        if (dialog.ShowDialog(this) != true) return;
        if (MessageBox.Show("Replace the companion library with this validated backup? Game files will not be changed.", "Restore library", MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
        try
        {
            var restored = await _store.ImportAsync(dialog.FileName);
            _allGames.Clear();
            foreach (var game in restored) _allGames.Add(game);
            RefreshPlatformOptions();
            RefreshVisibleGames();
            StatusText.Text = restored.Count + " game(s) restored";
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Restore failed", MessageBoxButton.OK, MessageBoxImage.Error); }
    }

    private async void AddGame_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog { Title = "Add a local Windows game", Filter = "Launchable files (*.exe;*.lnk;*.bat;*.cmd)|*.exe;*.lnk;*.bat;*.cmd", CheckFileExists = true };
        if (dialog.ShowDialog(this) != true) return;
        try
        {
            if (GameLibrary.ContainsLaunchTarget(_allGames, dialog.FileName))
                throw new InvalidDataException("This launch target is already in the library.");
            var game = GameLibrary.Create(Path.GetFileNameWithoutExtension(dialog.FileName), dialog.FileName);
            _allGames.Add(game);
            RefreshPlatformOptions();
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

    private void ShowFolder_Click(object sender, RoutedEventArgs e)
    {
        var game = Selected;
        if (game is null) return;
        try
        {
            var target = GameLibrary.ValidateExecutablePath(game.ExecutablePath);
            if (!File.Exists(target)) throw new FileNotFoundException("The configured file is missing.", target);
            Process.Start(new ProcessStartInfo { FileName = "explorer.exe", Arguments = "/select,\"" + target + "\"", UseShellExecute = true });
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Unable to show game", MessageBoxButton.OK, MessageBoxImage.Error); }
    }

    private async void Relocate_Click(object sender, RoutedEventArgs e)
    {
        var game = Selected;
        if (game is null) return;
        var dialog = new OpenFileDialog {
            Title = "Relocate " + game.Title,
            Filter = "Launchable files (*.exe;*.lnk;*.bat;*.cmd)|*.exe;*.lnk;*.bat;*.cmd",
            CheckFileExists = true
        };
        if (dialog.ShowDialog(this) != true) return;
        try
        {
            if (GameLibrary.ContainsLaunchTarget(_allGames, dialog.FileName, game.Id))
                throw new InvalidDataException("Another library entry already uses this launch target.");
            var replacement = GameLibrary.Relocate(game, dialog.FileName);
            Replace(game, replacement);
            await SaveLibraryAsync();
            StatusText.Text = "Relocated " + game.Title;
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Relocate failed", MessageBoxButton.OK, MessageBoxImage.Error); }
    }

    private async void Edit_Click(object sender, RoutedEventArgs e)
    {
        var game = Selected;
        if (game is null) return;
        var dialog = new LaunchMetadataDialog(game.Title, game.Platform, game.Arguments) { Owner = this };
        if (dialog.ShowDialog() != true) return;
        try
        {
            var replacement = GameLibrary.UpdateLaunchMetadata(game, dialog.GameTitle, dialog.Platform, dialog.Arguments);
            Replace(game, replacement);
            RefreshPlatformOptions();
            await SaveLibraryAsync();
            StatusText.Text = "Updated " + replacement.Title;
        }
        catch (Exception ex) { MessageBox.Show(ex.Message, "Unable to edit game", MessageBoxButton.OK, MessageBoxImage.Error); }
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
        var available = game is not null && GameLibrary.IsLaunchTargetAvailable(game);
        PlayButton.IsEnabled = available;
        if (game is not null && !available)
            SelectedPath.Text += Environment.NewLine + "Launch target is missing";
        EditButton.IsEnabled = game is not null;
        RelocateButton.IsEnabled = game is not null;
        ShowFolderButton.IsEnabled = available;
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
