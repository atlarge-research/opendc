import matplotlib.pyplot as plt

def generate_plot(multi_model):
    if multi_model.plot_type == "time_series":
        generate_time_series_plot(multi_model)
    elif multi_model.plot_type == "cumulative_total":
        generate_cumulative_plot(multi_model)
    elif multi_model.plot_type == "cumulative_time_series":
        generate_cumulative_time_series_plot(multi_model)
    save_plot(multi_model)

def generate_time_series_plot(multi_model):
    setup_time_series_plot(multi_model)
    plot_time_series(multi_model)

def generate_cumulative_plot(multi_model):
    setup_cumulative_plot()
    plot_cumulative()

def generate_cumulative_time_series_plot(multi_model):
    setup_cumulative_time_series_plot()
    plot_cumulative_time_series()

def setup_time_series_plot(multi_model):
    plt.figure(figsize=(10, 10))
    plt.title(multi_model.metric)
    plt.xlabel(multi_model.x_label)
    plt.ylim(multi_model.get_axis_lim())
    plt.ylabel(multi_model.metric + " " + multi_model.measure_unit)
    plt.grid()

def plot_time_series(multi_model):
    for model in multi_model.models:
        plt.plot(model.processed_host_data, label=("Model " + str(model.id) + "-" + model.experiment_name))
    plt.legend()

def save_plot(multi_model):
    folder_prefix = multi_model.output_folder_path + "/simulation-analysis/" + multi_model.metric + "/"
    plt.savefig(folder_prefix + "multimodel_metric=" + multi_model.metric + "_window=" + str(multi_model.window_size) + ".png")

def setup_cumulative_plot():
    print("Function setup_cumulative_plot not implemented yet
