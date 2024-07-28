"""
Goal: show how the window-size affects the visual comprehension / aspect of the plot.
Reproductiveness: run this experiment from main.py and use the parameters of
```experiments/more-windows-same-plot/outputs//window_size_comparison_same_plot```

In this experiment, we are plotting multiple window sizes in separate plots, and then in the same plot.
"""

def experiment_1(window_sizes=[1, 10, 100, 1000], metric="power_draw", max_samples=5000):
    analysis_file_path = utils.SIMULATION_ANALYSIS_FOLDER_NAME
    models_data = []

    plt.figure(figsize=(10, 10))
    plt.title("Power draw graph for different window sizes")

    for window_size in window_sizes:
        multimodel = MultiModel(metric, window_size)
        data_to_plot = multimodel.models[0].processed_sim_data
        data_to_plot = augment_data(data_to_plot, window_size)
        data_to_plot = [x / 1000 for x in data_to_plot][:max_samples]
        models_data.append(data_to_plot)

    for i, model_data in enumerate(models_data):
        plot_individual_model(
            export_path=analysis_file_path + "/window=" + str(window_sizes[i]) + ".svg",
            model_data=model_data,
            plot_title="Window size: " + str(window_sizes[i]),
        )

    plot_all_models(
        export_path=analysis_file_path + "/all_models_by_window.svg",
        models_data=models_data,
        plot_title="Power draw graph for different window sizes",
        window_sizes=window_sizes,
    )


def plot_individual_model(export_path, model_data, plot_title):
    plt.figure(figsize=(10, 10))
    plt.plot(model_data)
    plt.title(plot_title, fontsize=32)

    # Set the tick format for the x-axis
    ax = plt.gca()
    ax.xaxis.set_major_formatter(ticker.FuncFormatter(lambda x, pos: f'{int(x):,}'))

    plt.xticks(fontsize=32, ticks=[2000, 4000])
    plt.yticks(fontsize=32, ticks=[135, 140, 145])
    plt.ylim([131.5, 146])

    plt.fill_between([0, 5000], 131, 133, color='lightgray', alpha=0.2)

    # write a "~" on the gray area from above
    plt.text(-750, 132, "~", fontsize=32, color='black')
    plt.text(-750, 130.75, "0", fontsize=32, color='black')

    plt.tight_layout()
    plt.savefig(export_path, bbox_inches='tight')
    plt.close()


def plot_all_models(export_path, models_data, plot_title, window_sizes, with_zoom=True):
    fig, ax = plt.subplots(figsize=(20, 10))
    ax.set_title(plot_title, fontsize=32)
    ax.set_xticks([1000, 2000])
    ax.set_xticklabels([1000, 2000], fontsize=32)
    ax.set_yticks([135, 140, 145])
    ax.set_yticklabels([135, 140, 145], fontsize=32)
    ax.set_ylim([127, 147])
    ax.set_xlabel("Sample count", fontsize=32)
    ax.set_ylabel("Energy usage [kW]", fontsize=32)

    ax.fill_between([0, 2500], 127, 135, color='lightgray', alpha=0.2)
    ax.text(-250, 130, "~", fontsize=32, color='black')
    ax.text(-250, 126, "0", fontsize=32, color='black')

    if with_zoom:
        # Draw a square, fill transparently, borders pink
        coordinates_x = [1950, 1950, 2050, 2050, 1950]
        coordinates_y = [136, 139, 139, 136, 136]  # Shifted down by 3 units
        ax.plot(coordinates_x, coordinates_y, color='#FFD700', linewidth=2)
        ax.fill_betweenx([136, 139], 1950, 2050, color='#FFD700', alpha=0)

        # Create an inset plot
        inset_ax = fig.add_axes([0.6, 0.15, 0.35, 0.30])  # [left, bottom, width, height]
        inset_ax.set_xlim(1950, 2050)
        inset_ax.set_ylim(136, 139)  # Adjusted to match the shifted square
        inset_ax.set_title('Zoomed In', fontsize=24)
        inset_ax.set_xticks([])
        inset_ax.set_yticks([])

    colors = ['lightgray', 'black', 'green', 'blue']
    markers = ['o', 's', 'x', 'D']
    line_styles = ['-', '-', '-', ':']

    for i, model_data in enumerate(models_data):
        ax.plot(
            model_data[:2500],
            label="window of " + str(window_sizes[i]) + (" sample" if window_sizes[i] == 1 else " samples"),
            color=colors[i],
            marker=markers[i],
            linestyle=line_styles[i],
            markevery=500
        )
        if with_zoom:
            inset_ax.plot(
                model_data,
                label="w=" + str(window_sizes[i]),
                color=colors[i],
                marker=markers[i],
                linestyle=line_styles[i],
                markevery=50
            )

    ax.legend(fontsize=18)
    fig.tight_layout()  # Adjust layout to make sure everything fits well
    fig.savefig(export_path, bbox_inches='tight')
    plt.close(fig)  # Close the figure to free memory


"""
This function allows us to au®gment the data. Sample input:
window_size = 3
input: [a,b,c]
output: [a,a,a,b,b,b,c,c,c]
"""


def augment_data(data, window_size):
    augmented_data = []
    for value in data:
        for _ in range(window_size):
            augmented_data.append(value)

    return augmented_data
