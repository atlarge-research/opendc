from .models import MultiModel, MetaModel


def main():
    multimodel = MultiModel(
        read_input(sys.argv[1]),
        path=sys.argv[2],
    )

    multimodel.generate_plot()

    meta_model = MetaModel(multimodel)
    meta_model


if __name__ == "__main__":
    main()
